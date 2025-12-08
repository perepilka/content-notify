FROM python:3.11-slim

WORKDIR /app

# Install poetry as root
RUN pip install --no-cache-dir poetry==1.7.1

# Configure poetry to not create virtualenvs
RUN poetry config virtualenvs.create false

# Copy dependency files and install as root
COPY pyproject.toml ./
RUN poetry install --no-interaction --no-ansi --no-root --only main

# Create user with matching UID/GID for volume mount compatibility
RUN groupadd -g 1000 appuser && \
    useradd -r -u 1000 -g appuser appuser && \
    mkdir -p /home/appuser && \
    chown -R appuser:appuser /home/appuser /app

# Switch to non-root user
USER appuser

# Run the bot
CMD ["python", "-m", "src.main"]
