<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Application Name
    |--------------------------------------------------------------------------
    |
    | This value is the name of your application, which will be used when the
    | framework needs to place the application's name in a notification or
    | other UI elements where an application name needs to be displayed.
    |
    */

    'name' => env('APP_NAME', 'Laravel'),

    /*
    |--------------------------------------------------------------------------
    | Application Environment
    |--------------------------------------------------------------------------
    |
    | This value determines the "environment" your application is currently
    | running in. This may determine how you prefer to configure various
    | services the application utilizes. Set this in your ".env" file.
    |
    */

    'env' => env('APP_ENV', 'production'),

    /*
    |--------------------------------------------------------------------------
    | Application Debug Mode
    |--------------------------------------------------------------------------
    |
    | When your application is in debug mode, detailed error messages with
    | stack traces will be shown on every error that occurs within your
    | application. If disabled, a simple generic error page is shown.
    |
    */

    'debug' => (bool) env('APP_DEBUG', false),

    /*
    |--------------------------------------------------------------------------
    | Application URL
    |--------------------------------------------------------------------------
    |
    | This URL is used by the console to properly generate URLs when using
    | the Artisan command line tool. You should set this to the root of
    | the application so that it's available within Artisan commands.
    |
    */

    'url' => env('APP_URL', 'http://localhost'),

    /*
    |--------------------------------------------------------------------------
    | Application Timezone
    |--------------------------------------------------------------------------
    |
    | Here you may specify the default timezone for your application, which
    | will be used by the PHP date and date-time functions. The timezone
    | is set to "UTC" by default as it is suitable for most use cases.
    |
    */

    'timezone' => 'UTC',

    /*
    |--------------------------------------------------------------------------
    | Application Locale Configuration
    |--------------------------------------------------------------------------
    |
    | The application locale determines the default locale that will be used
    | by Laravel's translation / localization methods. This option can be
    | set to any locale for which you plan to have translation strings.
    |
    */

    'locale' => env('APP_LOCALE', 'en'),

    'fallback_locale' => env('APP_FALLBACK_LOCALE', 'en'),

    'faker_locale' => env('APP_FAKER_LOCALE', 'en_US'),

    /*
    |--------------------------------------------------------------------------
    | Encryption Key
    |--------------------------------------------------------------------------
    |
    | This key is utilized by Laravel's encryption services and should be set
    | to a random, 32 character string to ensure that all encrypted values
    | are secure. You should do this prior to deploying the application.
    |
    */

    'cipher' => 'AES-256-CBC',

    'key' => env('APP_KEY'),

    'previous_keys' => [
        ...array_filter(
            explode(',', (string) env('APP_PREVIOUS_KEYS', ''))
        ),
    ],

    /*
    |--------------------------------------------------------------------------
    | Maintenance Mode Driver
    |--------------------------------------------------------------------------
    |
    | These configuration options determine the driver used to determine and
    | manage Laravel's "maintenance mode" status. The "cache" driver will
    | allow maintenance mode to be controlled across multiple machines.
    |
    | Supported drivers: "file", "cache"
    |
    */

    'maintenance' => [
        'driver' => env('APP_MAINTENANCE_DRIVER', 'file'),
        'store' => env('APP_MAINTENANCE_STORE', 'database'),
    ],

    // Loan configuration
    'loan' => [
        'min_amount' => env('LOAN_MIN_AMOUNT', 1000),
        'max_amount' => env('LOAN_MAX_AMOUNT', 500000),
        'loan_expiry_days' => env('LOAN_EXPIRY_DAYS', 30),
        'currency' => env('LOAN_CURRENCY', 'TZS'),
        'default_percentage' => env('LOAN_DEFAULT_PERCENTAGE', 50),
        'max_outstanding_loans' => env('LOAN_MAX_OUTSTANDING', 1),
    ],

    // OTP configuration
    'otp' => [
        'length' => env('OTP_LENGTH', 6),
        'expiry_seconds' => env('OTP_EXPIRY_SECONDS', 300),
        'max_attempts' => env('OTP_MAX_ATTEMPTS', 5),
        'resend_cooldown_seconds' => env('OTP_RESEND_COOLDOWN', 60),
        'max_requests_per_hour' => env('OTP_MAX_REQUESTS_PER_HOUR', 5),
    ],

    // JWT configuration
    'jwt' => [
        'secret' => env('JWT_SECRET', 'lukuboost-jwt-secret-key-minimum-32-characters-long-for-hs256'),
        'access_token_expiry_minutes' => env('JWT_ACCESS_EXPIRY_MINUTES', 60),
        'refresh_token_expiry_days' => env('JWT_REFRESH_EXPIRY_DAYS', 7),
        'issuer' => env('JWT_ISSUER', 'lukuboost'),
    ],

    // CORS
    'cors_allowed_origins' => env('CORS_ALLOWED_ORIGINS', 'http://localhost:5173,http://localhost:3000'),

];
