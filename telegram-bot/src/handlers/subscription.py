"""Subscription management handlers for Telegram Bot."""
import logging
import re
from uuid import UUID

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message

from src.config import get_settings
from src.services.core_client import CoreClient, CoreAPIError

logger = logging.getLogger(__name__)

# Initialize router
router = Router()

# URL validation patterns (basic validation before sending to Core)
YOUTUBE_PATTERN = re.compile(r'^https?://(www\.)?youtube\.com/@[\w-]+$')
TWITCH_PATTERN = re.compile(r'^https?://(www\.)?twitch\.tv/[\w-]+$')

# Settings and client
settings = get_settings()
core_client = CoreClient(settings)

# In-memory storage for user account IDs (temporary solution)
# TODO: Replace with persistent storage (Redis/SQLite)
user_accounts: dict[int, UUID] = {}


async def get_or_create_account(telegram_id: int, username: str | None) -> UUID:
    """Get existing account ID or create new one.
    
    Args:
        telegram_id: Telegram user ID
        username: Telegram username
    
    Returns:
        UUID: Account ID from Core Service
    
    Raises:
        CoreAPIError: If registration fails
    """
    if telegram_id in user_accounts:
        return user_accounts[telegram_id]
    
    account_id = await core_client.register_user(telegram_id, username)
    user_accounts[telegram_id] = account_id
    logger.info(f"Cached account_id for telegram_id={telegram_id}")
    return account_id


def validate_url(url: str) -> bool:
    """Validate subscription URL format.
    
    Args:
        url: Channel URL to validate
    
    Returns:
        bool: True if URL matches YouTube or Twitch pattern
    """
    return bool(YOUTUBE_PATTERN.match(url) or TWITCH_PATTERN.match(url))


def extract_channel_name(url: str) -> str:
    """Extract channel name from URL for display.
    
    Args:
        url: Channel URL
    
    Returns:
        str: Channel name (username part of URL)
    
    Example:
        >>> extract_channel_name("https://www.youtube.com/@MrBeast")
        '@MrBeast'
    """
    if '@' in url:
        return '@' + url.split('@')[1]
    elif '/tv/' in url:
        return url.split('/tv/')[1]
    return url


@router.message(Command("add"))
async def cmd_add(message: Message) -> None:
    """Handle /add <url> command.
    
    Adds a new subscription for the user.
    
    Usage:
        /add https://www.youtube.com/@MrBeast
        /add https://www.twitch.tv/shroud
    
    Implements: FR-BOT-02 from PRD-Phase1.md
    """
    try:
        # Extract URL from message
        args = message.text.split(maxsplit=1) if message.text else []
        
        if len(args) < 2:
            await message.answer(
                "❌ <b>Usage:</b> /add &lt;url&gt;\n\n"
                "<b>Examples:</b>\n"
                "• /add https://www.youtube.com/@MrBeast\n"
                "• /add https://www.twitch.tv/shroud"
            )
            return
        
        url = args[1].strip()
        
        # Basic client-side validation
        if not validate_url(url):
            await message.answer(
                "❌ <b>Invalid URL format!</b>\n\n"
                "Supported formats:\n"
                "• YouTube: https://www.youtube.com/@username\n"
                "• Twitch: https://www.twitch.tv/username"
            )
            return
        
        # Get or create user account
        telegram_id = message.from_user.id
        username = message.from_user.username
        
        account_id = await get_or_create_account(telegram_id, username)
        
        # Add subscription via Core API
        result = await core_client.add_subscription(account_id, url)
        
        platform = result.get('platform', 'Unknown')
        channel_name = extract_channel_name(url)
        
        await message.answer(
            f"✅ <b>Subscription added!</b>\n\n"
            f"📺 Platform: <b>{platform}</b>\n"
            f"👤 Channel: <b>{channel_name}</b>\n"
            f"🔗 <a href='{url}'>View Channel</a>"
        )
        
        logger.info(
            f"Subscription added: user={telegram_id}, "
            f"platform={platform}, url={url}"
        )
    
    except CoreAPIError as e:
        logger.error(f"Core API error in /add: {e.message}")
        
        # Handle specific error cases
        if e.status_code == 409:
            await message.answer(
                "⚠️ <b>Already subscribed!</b>\n\n"
                "You're already following this channel."
            )
        elif e.status_code == 400:
            await message.answer(
                f"❌ <b>Invalid request:</b> {e.message}"
            )
        else:
            await message.answer(
                "❌ <b>Error:</b> System is temporarily unavailable. "
                "Please try again later."
            )
    
    except Exception as e:
        logger.error(f"Unexpected error in /add: {e}", exc_info=True)
        await message.answer(
            "❌ An unexpected error occurred. Please try again later."
        )


@router.message(Command("list"))
async def cmd_list(message: Message) -> None:
    """Handle /list command.
    
    Lists all active subscriptions for the user.
    
    Implements: FR-BOT-03 from PRD-Phase1.md
    """
    try:
        telegram_id = message.from_user.id
        username = message.from_user.username
        
        # Get or create user account
        account_id = await get_or_create_account(telegram_id, username)
        
        # Fetch subscriptions from Core API
        subscriptions = await core_client.get_subscriptions(account_id)
        
        if not subscriptions:
            await message.answer(
                "📭 <b>No subscriptions yet!</b>\n\n"
                "Add your first subscription with:\n"
                "/add &lt;channel_url&gt;"
            )
            return
        
        # Format subscriptions list
        response_lines = ["📋 <b>Your Subscriptions:</b>\n"]
        
        for idx, sub in enumerate(subscriptions, start=1):
            platform = sub.get('platform', 'Unknown')
            url = sub.get('channelUrl', '')
            sub_id = sub.get('id', '')
            channel_name = extract_channel_name(url)
            
            # Platform emoji
            platform_emoji = "📺" if platform == "YOUTUBE" else "🎮"
            
            response_lines.append(
                f"{idx}. {platform_emoji} <b>{channel_name}</b>\n"
                f"   Platform: {platform}\n"
                f"   <a href='{url}'>View Channel</a>\n"
                f"   ID: <code>{sub_id}</code>\n"
            )
        
        response_lines.append(
            f"\n<b>Total:</b> {len(subscriptions)} subscription(s)\n\n"
            "To remove a subscription, use:\n"
            "/remove &lt;id&gt;"
        )
        
        await message.answer("".join(response_lines))
        
        logger.info(
            f"Listed subscriptions: user={telegram_id}, "
            f"count={len(subscriptions)}"
        )
    
    except CoreAPIError as e:
        logger.error(f"Core API error in /list: {e.message}")
        await message.answer(
            "❌ <b>Error:</b> System is temporarily unavailable. "
            "Please try again later."
        )
    
    except Exception as e:
        logger.error(f"Unexpected error in /list: {e}", exc_info=True)
        await message.answer(
            "❌ An unexpected error occurred. Please try again later."
        )


@router.message(Command("remove"))
async def cmd_remove(message: Message) -> None:
    """Handle /remove <id> command.
    
    Removes a subscription by ID.
    
    Usage:
        /remove 123
    
    Implements: FR-BOT-04 from PRD-Phase1.md
    """
    try:
        # Extract subscription ID from message
        args = message.text.split(maxsplit=1) if message.text else []
        
        if len(args) < 2:
            await message.answer(
                "❌ <b>Usage:</b> /remove &lt;id&gt;\n\n"
                "Get subscription IDs with /list"
            )
            return
        
        # Validate ID is numeric
        try:
            subscription_id = int(args[1].strip())
        except ValueError:
            await message.answer(
                "❌ <b>Invalid ID!</b>\n\n"
                "Subscription ID must be a number.\n"
                "Use /list to see your subscription IDs."
            )
            return
        
        # Get user account
        telegram_id = message.from_user.id
        username = message.from_user.username
        
        account_id = await get_or_create_account(telegram_id, username)
        
        # Delete subscription via Core API
        await core_client.delete_subscription(subscription_id, account_id)
        
        await message.answer(
            f"✅ <b>Subscription removed!</b>\n\n"
            f"Subscription ID <code>{subscription_id}</code> has been deleted.\n\n"
            "View your remaining subscriptions with /list"
        )
        
        logger.info(
            f"Subscription removed: user={telegram_id}, "
            f"subscription_id={subscription_id}"
        )
    
    except CoreAPIError as e:
        logger.error(f"Core API error in /remove: {e.message}")
        
        if e.status_code == 404:
            await message.answer(
                "❌ <b>Subscription not found!</b>\n\n"
                "This subscription doesn't exist or you don't have access to it.\n"
                "Use /list to see your active subscriptions."
            )
        else:
            await message.answer(
                "❌ <b>Error:</b> System is temporarily unavailable. "
                "Please try again later."
            )
    
    except Exception as e:
        logger.error(f"Unexpected error in /remove: {e}", exc_info=True)
        await message.answer(
            "❌ An unexpected error occurred. Please try again later."
        )
