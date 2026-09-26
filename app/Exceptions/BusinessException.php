<?php

namespace App\Exceptions;

use Exception;

class BusinessException extends Exception
{
    protected string $errorCode;
    protected int $statusCode;

    public function __construct(string $errorCode, string $message, int $statusCode = 400)
    {
        parent::__construct($message);
        $this->errorCode = $errorCode;
        $this->statusCode = $statusCode;
    }

    public function getErrorCode(): string
    {
        return $this->errorCode;
    }

    public function getStatusCode(): int
    {
        return $this->statusCode;
    }

    public function render()
    {
        return response()->json([
            'success' => false,
            'error' => [
                'code' => $this->errorCode,
                'message' => $this->getMessage(),
            ],
            'data' => null,
        ], $this->statusCode);
    }
}
