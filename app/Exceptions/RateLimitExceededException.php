<?php

namespace App\Exceptions;

class RateLimitExceededException extends BusinessException
{
    public function __construct(string $message = 'Rate limit exceeded.')
    {
        parent::__construct('RATE_LIMIT_EXCEEDED', $message, 429);
    }
}
