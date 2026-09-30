package net.dimaskama.voicemessagestranscriber.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.dimaskama.voicemessagestranscriber.screen.TranscriberSettingsScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TranscriberSettingsScreen::new;
    }

}
