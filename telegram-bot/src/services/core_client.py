"""Client for communicating with the Core Service API."""
import logging
from typing import Any
from uuid import UUID

import aiohttp
from aiohttp import ClientError, ClientResponseError

from src.config import Settings

logger = logging.getLogger(__name__)


class CoreAPIError(Exception):
    """Base exception for Core API errors."""
    
    def __init__(self, message: str, status_code: int | None = None) -> None:
        self.message = message
        self.status_code = status_code
        super().__init__(self.message)


class CoreClient:
    """Async HTTP client for Core Service API.
    
    Implements the API contract from PRD-Phase1.md Section 5.
    Handles authentication, subscription management, and error responses.
    """
    
    def __init__(self, settings: Settings) -> None:
        """Initialize Core API client.
        
        Args:
            settings: Application settings containing core_api_url
        """
        self.base_url = settings.core_api_url
        self.timeout = aiohttp.ClientTimeout(total=10)
    
    async def register_user(self, telegram_id: int, username: str | None = None) -> UUID:
        """Register or retrieve a user account.
        
        Implements: POST /api/v1/users/auth (PRD Section 5.1)
        
        Args:
            telegram_id: Telegram user ID (providerId)
            username: Telegram username (optional)
        
        Returns:
            UUID: Account ID from Core Service
        
        Raises:
            CoreAPIError: If the API returns an error or is unavailable
        
        Example:
            >>> account_id = await client.register_user(123456789, "john_doe")
            >>> print(account_id)
            550e8400-e29b-41d4-a716-446655440000
        """
        url = f"{self.base_url}/users/auth"
        payload = {
            "provider": "TELEGRAM",
            "providerId": str(telegram_id),
            "username": username
        }
        
        try:
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.post(url, json=payload) as response:
                    response.raise_for_status()
                    data = await response.json()
                    
                    account_id = data.get("accountId")
                    if not account_id:
                        raise CoreAPIError("Missing accountId in response")
                    
                    logger.info(
                        f"User registered: telegram_id={telegram_id}, "
                        f"account_id={account_id}, is_new={data.get('isNew', False)}"
                    )
                    return UUID(account_id)
        
        except ClientResponseError as e:
            await self._handle_http_error(e, "register user")
        except ClientError as e:
            logger.error(f"Network error during user registration: {e}")
            raise CoreAPIError("System is temporarily unavailable, please try again later")
        except Exception as e:
            logger.error(f"Unexpected error during user registration: {e}")
            raise CoreAPIError("An unexpected error occurred")
    
    async def add_subscription(self, account_id: UUID, url: str) -> dict[str, Any]:
        """Add a new subscription for a user.
        
        Implements: POST /api/v1/subscriptions (PRD Section 5.2)
        
        Args:
            account_id: User's account UUID
            url: YouTube or Twitch channel URL
        
        Returns:
            dict: Subscription response containing id, platform, channelUrl
        
        Raises:
            CoreAPIError: If the URL is invalid or API returns an error
        
        Example:
            >>> result = await client.add_subscription(
            ...     UUID("550e8400-e29b-41d4-a716-446655440000"),
            ...     "https://www.youtube.com/@MrBeast"
            ... )
            >>> print(result["platform"])
            YOUTUBE
        """
        url_endpoint = f"{self.base_url}/subscriptions"
        payload = {
            "accountId": str(account_id),
            "url": url
        }
        
        try:
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.post(url_endpoint, json=payload) as response:
                    response.raise_for_status()
                    data = await response.json()
                    
                    logger.info(
                        f"Subscription added: account_id={account_id}, "
                        f"platform={data.get('platform')}, url={url}"
                    )
                    return data
        
        except ClientResponseError as e:
            await self._handle_http_error(e, "add subscription")
        except ClientError as e:
            logger.error(f"Network error during adding subscription: {e}")
            raise CoreAPIError("System is temporarily unavailable, please try again later")
        except Exception as e:
            logger.error(f"Unexpected error during adding subscription: {e}")
            raise CoreAPIError("An unexpected error occurred")
    
    async def get_subscriptions(self, account_id: UUID) -> list[dict[str, Any]]:
        """Get all subscriptions for a user.
        
        Implements: GET /api/v1/subscriptions/{accountId} (PRD Section 5.2)
        
        Args:
            account_id: User's account UUID
        
        Returns:
            list[dict]: List of subscriptions with id, platform, channelUrl
        
        Raises:
            CoreAPIError: If the API returns an error
        
        Example:
            >>> subscriptions = await client.get_subscriptions(
            ...     UUID("550e8400-e29b-41d4-a716-446655440000")
            ... )
            >>> for sub in subscriptions:
            ...     print(f"{sub['platform']}: {sub['channelUrl']}")
            YOUTUBE: https://www.youtube.com/@MrBeast
        """
        url = f"{self.base_url}/subscriptions/{account_id}"
        
        try:
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.get(url) as response:
                    response.raise_for_status()
                    data = await response.json()
                    
                    logger.info(
                        f"Retrieved subscriptions: account_id={account_id}, "
                        f"count={len(data)}"
                    )
                    return data
        
        except ClientResponseError as e:
            await self._handle_http_error(e, "get subscriptions")
        except ClientError as e:
            logger.error(f"Network error during getting subscriptions: {e}")
            raise CoreAPIError("System is temporarily unavailable, please try again later")
        except Exception as e:
            logger.error(f"Unexpected error during getting subscriptions: {e}")
            raise CoreAPIError("An unexpected error occurred")
    
    async def delete_subscription(
        self, 
        subscription_id: int, 
        account_id: UUID
    ) -> None:
        """Delete a subscription.
        
        Implements: DELETE /api/v1/subscriptions/{id}?accountId={accountId}
        
        Args:
            subscription_id: Subscription ID to delete
            account_id: User's account UUID (for ownership validation)
        
        Raises:
            CoreAPIError: If the subscription doesn't exist or access denied
        
        Example:
            >>> await client.delete_subscription(1, UUID("550e8400-..."))
        """
        url = f"{self.base_url}/subscriptions/{subscription_id}"
        params = {"accountId": str(account_id)}
        
        try:
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.delete(url, params=params) as response:
                    response.raise_for_status()
                    
                    logger.info(
                        f"Subscription deleted: id={subscription_id}, "
                        f"account_id={account_id}"
                    )
        
        except ClientResponseError as e:
            await self._handle_http_error(e, "delete subscription")
        except ClientError as e:
            logger.error(f"Network error during deleting subscription: {e}")
            raise CoreAPIError("System is temporarily unavailable, please try again later")
        except Exception as e:
            logger.error(f"Unexpected error during deleting subscription: {e}")
            raise CoreAPIError("An unexpected error occurred")
    
    async def _handle_http_error(self, error: ClientResponseError, operation: str) -> None:
        """Handle HTTP errors from Core API.
        
        Implements: Standard Error Response (PRD Section 6.2)
        
        Args:
            error: The HTTP error from aiohttp
            operation: Description of the operation that failed
        
        Raises:
            CoreAPIError: With appropriate error message
        """
        try:
            error_data = await error.response.json()
            error_message = error_data.get("message", "Unknown error")
            logger.error(
                f"Core API error during {operation}: "
                f"status={error.status}, message={error_message}"
            )
        except Exception:
            error_message = f"HTTP {error.status} error"
            logger.error(f"Core API error during {operation}: {error.status}")
        
        # Map status codes to user-friendly messages
        if error.status == 400:
            raise CoreAPIError(error_message, error.status)
        elif error.status == 404:
            raise CoreAPIError("Resource not found", error.status)
        elif error.status == 409:
            raise CoreAPIError(error_message, error.status)
        elif error.status >= 500:
            raise CoreAPIError(
                "System is temporarily unavailable, please try again later",
                error.status
            )
        else:
            raise CoreAPIError(error_message, error.status)
