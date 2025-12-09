"""Telegram Bot entry point for StreamNexus."""
import asyncio
import logging

from aiohttp import web
from aiogram import Bot, Dispatcher
from aiogram.client.default import DefaultBotProperties
from aiogram.enums import ParseMode
from aiogram.filters import Command, CommandStart
from aiogram.types import Message

from src.config import get_settings
from src.handlers import subscription
from src.routes import setup_routes

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

settings = get_settings()

bot = Bot(
    token=settings.bot_token.get_secret_value(),
    default=DefaultBotProperties(parse_mode=ParseMode.HTML)
)
dp = Dispatcher()


@dp.message(CommandStart())
async def cmd_start(message: Message) -> None:
    """Handle /start command.
    
    Implements: FR-BOT-01 from PRD-Phase1.md
    """
    username = message.from_user.username or "there"
    
    await message.answer(
        f"👋 <b>Hello, {username}!</b>\n\n"
        "I'm <b>StreamNexus Bot</b> - your personal stream notification assistant!\n\n"
        "📺 <b>Available Commands:</b>\n"
        "• /add &lt;url&gt; - Subscribe to a channel\n"
        "• /list - View your subscriptions\n"
        "• /remove &lt;id&gt; - Unsubscribe from a channel\n"
        "• /help - Show this help message\n\n"
        "🎯 <b>Supported Platforms:</b>\n"
        "• YouTube (@username)\n"
        "• Twitch (/username)\n\n"
        "Get started by adding your first subscription!\n"
        "Example: /add https://www.youtube.com/@MrBeast"
    )
    
    logger.info(f"User started bot: telegram_id={message.from_user.id}")


@dp.message(Command("help"))
async def cmd_help(message: Message) -> None:
    """Handle /help command."""
    await message.answer(
        "📖 <b>StreamNexus Bot - Help</b>\n\n"
        "<b>Commands:</b>\n\n"
        "📌 <b>/add</b> &lt;url&gt;\n"
        "Subscribe to a YouTube or Twitch channel.\n"
        "<i>Example: /add https://www.youtube.com/@MrBeast</i>\n\n"
        "📌 <b>/list</b>\n"
        "View all your active subscriptions.\n\n"
        "📌 <b>/remove</b> &lt;id&gt;\n"
        "Unsubscribe from a channel using its ID.\n"
        "<i>Get the ID from /list command.</i>\n\n"
        "📌 <b>/help</b>\n"
        "Show this help message.\n\n"
        "<b>Supported URL Formats:</b>\n"
        "• YouTube: https://www.youtube.com/@username\n"
        "• Twitch: https://www.twitch.tv/username\n\n"
        "Need assistance? Contact support!"
    )


async def create_web_app() -> web.Application:
    """Create and configure the aiohttp web application.
    
    Returns:
        Configured web.Application for internal API
    """
    app = web.Application()
    
    # Store bot and settings in app context for routes
    app['bot'] = bot
    app['settings'] = settings
    
    # Register internal API routes
    setup_routes(app)
    
    return app


async def start_web_server(app: web.Application, port: int) -> web.AppRunner:
    """Start the aiohttp web server.
    
    Args:
        app: Web application
        port: Port to listen on
    
    Returns:
        AppRunner instance
    """
    runner = web.AppRunner(app)
    await runner.setup()
    
    site = web.TCPSite(runner, '0.0.0.0', port)
    await site.start()
    
    logger.info(f"Internal API server started on port {port}")
    return runner


async def on_startup() -> None:
    """Actions to perform on bot startup."""
    logger.info("Bot is starting up...")
    logger.info(f"Core API URL: {settings.core_api_url}")
    logger.info(f"Webhook port: {settings.webhook_port}")
    
    if settings.internal_service_key:
        logger.info("Internal service authentication: ENABLED")
    else:
        logger.warning("Internal service authentication: DISABLED (not recommended for production)")


async def on_shutdown() -> None:
    """Actions to perform on bot shutdown."""
    logger.info("Bot is shutting down...")
    await bot.session.close()


async def main() -> None:
    """Initialize and start the bot with internal API server.
    
    Runs two concurrent tasks:
    1. Telegram bot polling (for user commands)
    2. HTTP server (for internal notifications from Core Service)
    """
    # Register routers
    dp.include_router(subscription.router)
    
    # Register startup/shutdown hooks
    dp.startup.register(on_startup)
    dp.shutdown.register(on_shutdown)
    
    # Create web application for internal API
    web_app = await create_web_app()
    
    # Start web server
    runner = await start_web_server(web_app, settings.webhook_port)
    
    try:
        # Start bot polling
        logger.info("Starting bot polling...")
        await dp.start_polling(bot)
    finally:
        # Cleanup web server on shutdown
        logger.info("Stopping web server...")
        await runner.cleanup()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Bot stopped by user")
    except Exception as e:
        logger.error(f"Fatal error: {e}", exc_info=True)
