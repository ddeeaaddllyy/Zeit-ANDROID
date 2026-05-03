
pub enum Command {
    Start,
    Help,
    Music,
    AboutProject,
    Unknown
}

impl Command {
    pub fn from_text(text: &str) -> Self {
        match text {
            "/start" => Command::Start,
            "/help" => Command::Help,
            "/music" => Command::Music,
            "/aboutproject" => Command::AboutProject,
            _ => Command::Unknown
        }
    }
}