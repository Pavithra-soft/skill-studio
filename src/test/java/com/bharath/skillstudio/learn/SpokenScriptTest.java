package com.bharath.skillstudio.learn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpokenScriptTest {

    @Test
    void skipsCodeAndCommandsButKeepsTheStory() {
        String spoken = SpokenScript.sanitize("""
                Virtual threads are cheaper for blocking work.
                Thread.startVirtualThread(() -> handle(request));
                curl -X POST https://example.com/apply
                kubectl apply -f deploy.yaml
                In production a servlet can stay blocking.
                """);
        assertThat(spoken).contains("Virtual threads");
        assertThat(spoken).contains("In production");
        assertThat(spoken).doesNotContain("startVirtualThread");
        assertThat(spoken).doesNotContain("curl");
        assertThat(spoken).doesNotContain("kubectl");
        assertThat(spoken).doesNotContain("example.com");
    }

    @Test
    void splitsCamelCaseForTheEar() {
        assertThat(SpokenScript.sanitize("ChatClient is the door.")).contains("Chat Client");
    }

    @Test
    void doesNotReadCveIds() {
        assertThat(SpokenScript.sanitize("See CVE-2026-53493 before you bump."))
                .contains("security advisory")
                .doesNotContain("CVE-2026");
    }
}
