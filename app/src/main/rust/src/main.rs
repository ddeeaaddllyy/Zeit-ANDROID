use reqwest::Client;
use serde::Deserialize;

#[derive(Deserialize)]
struct Response {
    result: Vec<Update>,
}

#[derive(Deserialize)]
struct Update {
    update_id: u64,
    message: Option<Message>,
}

#[derive(Deserialize)]
struct Message {
    chat: Chat,
    text: Option<String>,
}

#[derive(Deserialize)]
struct Chat {
    id: u64,
}

#[tokio::main(flavor = "current_thread")]
async fn main() {
    let token = "8579022330:AAEK8t2B4WquvZo2oEDhZJloVjoY-b6a0W4";
    let client = reqwest::Client::new();

    let mut offset = 0;

    loop {
        let url = format!(
            "https://api.telegram.org/bot{}/getUpdates?timeout=10&offset={}",
            token, offset
        );

        let resp: Response = client
            .get(&url)
            .send()
            .await
            .unwrap()
            .json()
            .await
            .unwrap();

        for update in resp.result {
            offset = update.update_id + 1;

            if let Some(msg) = update.message {
                if let Some(text) = msg.text {
                    if text == "/start" {
                        send_message(&client, token, msg.chat.id as i64, "hi").await;
                    }
                }
            }
        }
    }
}

async fn send_message(client: &Client, token: &str, chat_id: i64, text: &str) {
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