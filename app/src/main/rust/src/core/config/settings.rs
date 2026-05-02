pub struct Settings {
    pub telegram_bot_token: String,
    pub discord_bot_token: String
}

impl Settings {
    pub fn new() -> Self {
        Self {
            telegram_bot_token: std::env::var("TELEGRAM_TOKEN").unwrap(),
            discord_bot_token: std::env::var("DISCORD_TOKEN").unwrap()
        }
    }
}