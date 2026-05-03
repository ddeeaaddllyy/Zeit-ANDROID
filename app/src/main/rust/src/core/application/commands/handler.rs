use super::command::{Command};

pub fn handle_command(command: Command) -> String {
    match command {
        Command::Start => {
            "HII".to_string()
        },
        Command::Help => {
            "Hi this is new bot".to_string()
        },
        Command::Music => {
            "In Process".to_string()
        },
        Command::AboutProject => {
            "This is project created by ddeeaaddllyy \
            for git progect Zeit. Follow me in git".to_string()
        }
        Command::Unknown => {
            "".to_string()
        },
    }
}