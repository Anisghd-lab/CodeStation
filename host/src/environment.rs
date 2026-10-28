//! Build identity. Development and production must never own the same service or data.

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Environment {
    Main,
    Dev,
}

impl Environment {
    pub const fn current() -> Self {
        match option_env!("CODESTATION_ENV") {
            Some(value) => match value.as_bytes() {
                b"dev" => Self::Dev,
                _ => Self::Main,
            },
            None => match option_env!("CODYNC_ENV") {
                Some(value) => match value.as_bytes() {
                    b"dev" => Self::Dev,
                    _ => Self::Main,
                },
                None => Self::Main,
            },
        }
    }

    pub const fn name(self) -> &'static str {
        match self {
            Self::Main => "main",
            Self::Dev => "dev",
        }
    }

    pub const fn port(self) -> u16 {
        match self {
            Self::Main => 19222,
            Self::Dev => 19223,
        }
    }

    pub const fn data_folder(self) -> &'static str {
        match self {
            Self::Main => ".codestation",
            Self::Dev => ".codestation-dev",
        }
    }

    pub const fn url_scheme(self) -> &'static str {
        match self {
            Self::Main => "codestation",
            Self::Dev => "codestation-dev",
        }
    }

}

#[cfg(test)]
mod tests {
    use super::Environment::{Dev, Main};

    #[test]
    fn development_cannot_replace_production_resources() {
        assert_eq!(Main.port(), 19222);
        assert_eq!(Main.data_folder(), ".codestation");
        assert_ne!(Main.port(), Dev.port());
        for (main, dev) in [(Main.data_folder(), Dev.data_folder()), (Main.url_scheme(), Dev.url_scheme())] {
            assert_ne!(main, dev);
        }
    }
}
