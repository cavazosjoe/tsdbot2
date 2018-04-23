package org.tsd.tsdbot.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.tsd.tsdbot.discord.DiscordMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiscUtilsTest {

    @Test
    public void testSanitizeMessage() {
        DiscordMessage message = mock(DiscordMessage.class);
        when(message.getContent())
                .thenReturn("here is some text <:emoji:13489031234> https://www.youtube.com/yes/?huhhh=3.jpg boy howdy");
        String sanitized = MiscUtils.getSanitizedContent(message);
        assertThat(sanitized, is("here is some text boy howdy"));
    }
}
