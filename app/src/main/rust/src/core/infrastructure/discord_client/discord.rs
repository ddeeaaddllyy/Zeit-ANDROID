use serenity::{Client, prelude::*};
use crate::core::utils::debug::log;

struct Handler;

#[serenity::async_trait]
impl EventHandler for Handler {}

pub async fn start(token: String) {
    let intents: GatewayIntents = GatewayIntents::GUILD_MESSAGES | GatewayIntents::MESSAGE_CONTENT;

    let mut client: Client = Client::builder(token, intents)
        .event_handler(Handler)
        .await
        .expect("Discord client error");

    log("Discord bot started");

    if let Err(e) = client.start().await {
        log(&format!("Discord error: {:?}", e));
    }
}