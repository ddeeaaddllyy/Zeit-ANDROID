use serde::Deserialize;

#[derive(Deserialize)]
pub struct Response {
    pub(crate) result: Vec<Update>,
}

#[derive(Deserialize)]
pub struct Update {
    pub(crate) update_id: u64,
    pub(crate) message: Option<Message>,
}

#[derive(Deserialize)]
pub struct Message {
    pub(crate) chat: Chat,
    pub(crate) text: Option<String>,
}

#[derive(Deserialize)]
pub struct Chat {
    pub(crate) id: u64,
}