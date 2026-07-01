ALTER TABLE teachers
    ADD COLUMN home_shortcut_show_description BIT(1) NOT NULL DEFAULT b'1' AFTER home_shortcuts;
