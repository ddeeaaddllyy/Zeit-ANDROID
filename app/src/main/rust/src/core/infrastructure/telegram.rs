use crate::core::utils::debug::log;

pub async fn start(token: String) {
    log(&format!("Telegram bot started with token: {}", token));

    loop {
        tokio::time::sleep(std::time::Duration::from_secs(5)).await;
        log("Telegram polling...");
    }
}