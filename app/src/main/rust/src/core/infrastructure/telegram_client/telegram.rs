use crate::core::utils::debug::log;
use crate::core::application::commands::{handler};
use crate::core::application::commands::command::Command;
use crate::core::infrastructure::telegram_client::telegram_responce::{Response, Update};

pub async fn start(token: String) {
    let client = reqwest::Client::new();
    let mut offset = 0;

    log("Telegram bot started");

    loop {
        let url = format!(
            "https://api.telegram.org/bot{}/getUpdates?timeout=10&offset={}",
            token, offset
        );

        let resp = client.get(&url).send().await;

        if resp.is_err() {
            log("Request error");
            continue;
        }

        let text = resp.unwrap().text().await.unwrap();

        let parsed: Result<Response<Update>, _> = serde_json::from_str(&text);

        if parsed.is_err() {
            log("JSON parse error");
            continue;
        }

        let data = parsed.unwrap();

        if !data.ok {
            log("Telegram API error");
            continue;
        }

        let updates = data.result.unwrap_or_default();

        for update in updates {
            offset = update.update_id + 1;

            if let Some(msg) = update.message {
                if let Some(text) = msg.text {
                    let command = Command::from_text(&text);
                    let answer = handler::handle_command(command);

                    send_message(&client, &token, msg.chat.id, &answer).await;
                }
            }
        }
    }
}

async fn send_message(client: &reqwest::Client, token: &str, chat_id: i64, text: &str) {
    let url = format!("https://api.telegram.org/bot{}/sendMessage", token);

    let _ = client
        .post(&url)
        .json(&serde_json::json!({
            "chat_id": chat_id,
            "text": text
        }))
        .send()
        .await;
}