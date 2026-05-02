use crate::core::application::runner::run;

mod core;

#[tokio::main(flavor = "current_thread")]
async fn main() {
    run().await;
}