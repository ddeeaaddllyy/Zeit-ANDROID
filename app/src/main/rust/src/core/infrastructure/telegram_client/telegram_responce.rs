use serde::Deserialize;

#[derive(Deserialize, Debug)]
pub struct Response<Update> {
    pub(crate) ok: bool,
    pub(crate) result: Option<Vec<Update>>,
}

#[derive(Deserialize, Debug)]
pub struct Update {
    pub(crate) update_id: i64,
    pub(crate) message: Option<Message>,
}

#[derive(Deserialize, Debug)]
pub struct Message {
    pub(crate) chat: Chat,
    pub(crate) text: Option<String>,
}

#[derive(Deserialize, Debug)]
pub struct Chat {
    pub(crate) id: i64,
}