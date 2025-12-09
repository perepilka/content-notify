"""Internal API routes for receiving notifications from Core Service."""
import logging
from typing import Any

from aiohttp import web
from aiogram import Bot

from src.config import Settings

logger = logging.getLogger(__name__)


async def handle_notification(request: web.Request) -> web.Response:
    """Handle internal notification from Core Service.
    
    Implements: FR-BOT-05 from PRD-Phase1.md
    POST /internal/send
    
    Headers:
        X-Internal-Service-Key: Shared secret for authentication
    
    Body:
        {
            "chatId": "123456789",
            "message": "🔴 **MrBeast** is live!\n\nLink: https://youtu.be/xyz"
        }
    
    Returns:
        200 OK: Message sent successfully
        403 Forbidden: Invalid authentication key
        400 Bad Request: Missing required fields
        500 Internal Server Error: Failed to send message
    """
    settings: Settings = request.app['settings']
    bot: Bot = request.app['bot']
    
    # Authentication: Check X-Internal-Service-Key header
    auth_header = request.headers.get('X-Internal-Service-Key')
    
    if not settings.internal_service_key:
        logger.warning("INTERNAL_SERVICE_KEY not configured - rejecting request")
        return web.json_response(
            {'error': 'Internal service authentication not configured'},
            status=500
        )
    
    expected_key = settings.internal_service_key.get_secret_value()
    
    if not auth_header or auth_header != expected_key:
        logger.warning(
            f"Invalid authentication attempt from {request.remote} "
            f"- header present: {bool(auth_header)}"
        )
        return web.json_response(
            {'error': 'Forbidden: Invalid authentication key'},
            status=403
        )
    
    # Parse request body
    try:
        data = await request.json()
    except Exception as e:
        logger.error(f"Failed to parse JSON body: {e}")
        return web.json_response(
            {'error': 'Invalid JSON body'},
            status=400
        )
    
    # Validate required fields
    chat_id = data.get('chatId')
    message = data.get('message')
    
    if not chat_id:
        return web.json_response(
            {'error': 'Missing required field: chatId'},
            status=400
        )
    
    if not message:
        return web.json_response(
            {'error': 'Missing required field: message'},
            status=400
        )
    
    # Convert chatId to int (handle both string and int)
    try:
        chat_id_int = int(chat_id)
    except (ValueError, TypeError):
        return web.json_response(
            {'error': f'Invalid chatId format: must be numeric'},
            status=400
        )
    
    # Send message to user
    try:
        await bot.send_message(
            chat_id=chat_id_int,
            text=message,
            parse_mode='HTML'
        )
        
        logger.info(
            f"Notification sent successfully: chat_id={chat_id_int}, "
            f"message_length={len(message)}"
        )
        
        return web.json_response({
            'status': 'success',
            'message': 'Notification sent'
        })
    
    except Exception as e:
        logger.error(
            f"Failed to send message to chat_id={chat_id_int}: {e}",
            exc_info=True
        )
        return web.json_response(
            {'error': f'Failed to send message: {str(e)}'},
            status=500
        )


async def health_check(request: web.Request) -> web.Response:
    """Health check endpoint for monitoring.
    
    Returns:
        200 OK: Service is healthy
    """
    return web.json_response({
        'status': 'healthy',
        'service': 'telegram-bot'
    })


def setup_routes(app: web.Application) -> None:
    """Register internal API routes.
    
    Args:
        app: aiohttp web application
    """
    app.router.add_post('/internal/send', handle_notification)
    app.router.add_get('/health', health_check)
    
    logger.info("Internal API routes registered")
