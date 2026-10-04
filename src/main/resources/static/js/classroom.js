(() => {
    const PAGE_SIZE = 4;

    const state = {
        lessonTitle: "",
        lessonVoice: "",
        speaking: false,
        paused: false,
        speakGen: 0,
        rate: 0.84,
        beats: [],
        beatIndex: 0,
        title: "",
        skillKey: "",
        skillName: "",
        conceptSlug: "",
        page: 0,
        tab: "overview",
        lesson: null,
        conceptIndex: [],
        basket: { skills: {}, concepts: {} }
    };

    function $(id) {
        return document.getElementById(id);
    }

    function voices() {
        return window.speechSynthesis ? window.speechSynthesis.getVoices() : [];
    }

    function pickVoice(utterance) {
        const list = voices();
        const ranked = list
            .map((voice) => {
                const name = voice.name.toLowerCase();
                const lang = (voice.lang || "").toLowerCase();
                let score = 0;
                if (lang.startsWith("en")) {
                    score += 12;
                }
                if (/en-gb|en-us|en-in|en-au/.test(lang)) {
                    score += 8;
                }
                if (/google uk english|google us english|samantha|daniel|karen|moira|rishi|serena|arthur|natural|premium|enhanced|neural/.test(name)) {
                    score += 24;
                }
                if (/female|male/.test(name)) {
                    score += 4;
                }
                if (/compact|eloquence/.test(name)) {
                    score -= 8;
                }
                return { voice, score };
            })
            .sort((a, b) => b.score - a.score);
        const best = ranked[0] && ranked[0].score > 0 ? ranked[0].voice : list.find((voice) => /^en/i.test(voice.lang));
        if (best) {
            utterance.voice = best;
            utterance.lang = best.lang;
        } else {
            utterance.lang = "en-IN";
        }
    }

    function splitBeats(text) {
        return String(text || "")
            .replace(/\s*\.\.\.\s*/g, "|")
            .replace(/([.!?])\s+/g, "$1|")
            .split("|")
            .map((part) => part.trim())
            .filter((part) => part.length > 1 && !looksLikeCode(part) && !looksLikeCommand(part));
    }

    function looksLikeCode(sentence) {
        const text = String(sentence || "").trim();
        if (text.length < 2) {
            return true;
        }
        if (/[{}]|=>/.test(text)) {
            return true;
        }
        if (/\(.*\)/.test(text) && /[A-Za-z0-9]\.[A-Za-z0-9].*\(/.test(text)) {
            return true;
        }
        if (/\w+\.\w+\.\w+.*=/.test(text)) {
            return true;
        }
        return text.includes("();") || text.startsWith("//");
    }

    function looksLikeCommand(sentence) {
        const text = String(sentence || "").trim().toLowerCase();
        if (/^(curl|kubectl|mvn|docker|git|npm|helm|aws|http)\b/.test(text) || text.startsWith("--")) {
            return true;
        }
        return false;
    }

    function sanitizeForVoice(text) {
        return String(text || "")
            .replace(/```[\s\S]*?```/g, " ")
            .replace(/`[^`]*`/g, " ")
            .replace(/https?:\/\/\S+/g, " ")
            .replace(/\bCVE-\d{4}-\d+\b/gi, "a security advisory")
            .replace(/\b[A-Z][A-Z0-9]+-\d+\b/g, " ")
            .replace(/#\d+/g, " ")
            .replace(/\b[a-z0-9_]*api[_-]?key\b/gi, "the API key")
            .replace(/_/g, " ")
            .replace(/([a-z])([A-Z])/g, "$1 $2")
            .replace(/([A-Z])([A-Z][a-z])/g, "$1 $2")
            .replace(/([A-Za-z])\.([A-Za-z])/g, "$1 $2");
    }

    function stopSpeech() {
        state.speakGen += 1;
        if (window.speechSynthesis) {
            window.speechSynthesis.cancel();
        }
        state.speaking = false;
        state.paused = false;
        state.beats = [];
        state.beatIndex = 0;
        document.querySelectorAll(".learn-block.speaking").forEach((el) => el.classList.remove("speaking"));
        setVoiceStatus("Stopped.");
        syncVoiceButtons();
    }

    function pauseSpeech() {
        if (!state.speaking && !state.paused) {
            return;
        }
        state.paused = true;
        state.speaking = false;
        if (window.speechSynthesis) {
            window.speechSynthesis.cancel();
        }
        setVoiceStatus("Paused. Resume when you want the next beat.");
        syncVoiceButtons();
    }

    function resumeSpeech() {
        if (!state.paused || !state.beats.length) {
            return;
        }
        state.paused = false;
        speakFrom(state.beatIndex, state.speakGen);
    }

    function setVoiceStatus(text) {
        document.querySelectorAll(".js-voice-status").forEach((el) => {
            el.textContent = text;
        });
    }

    function hasVoice() {
        return Boolean(state.lessonVoice);
    }

    function syncVoiceButtons() {
        document.querySelectorAll(".js-voice-play").forEach((play) => {
            play.disabled = !hasVoice();
        });
        document.querySelectorAll(".js-voice-pause").forEach((pause) => {
            pause.disabled = !state.speaking && !state.paused;
            pause.textContent = state.paused ? "Resume" : "Pause";
        });
        document.querySelectorAll(".js-voice-stop").forEach((stop) => {
            stop.disabled = !state.speaking && !state.paused;
        });
    }

    function highlightSection(index, total) {
        const blocks = document.querySelectorAll(".learn-block");
        if (!blocks.length || total <= 0) {
            return;
        }
        const slot = Math.min(blocks.length - 1, Math.floor((index / total) * blocks.length));
        blocks.forEach((el, i) => el.classList.toggle("speaking", i === slot));
    }

    function speakFrom(startIndex, myGen) {
        const go = (index) => {
            if (state.speakGen !== myGen || state.paused) {
                return;
            }
            if (index >= state.beats.length) {
                state.speaking = false;
                state.paused = false;
                document.querySelectorAll(".learn-block.speaking").forEach((el) => el.classList.remove("speaking"));
                setVoiceStatus("That's the huddle: " + state.title);
                syncVoiceButtons();
                return;
            }
            state.beatIndex = index;
            highlightSection(index, state.beats.length);
            const utterance = new SpeechSynthesisUtterance(state.beats[index]);
            utterance.rate = state.rate;
            utterance.pitch = 1.02;
            utterance.volume = 1;
            pickVoice(utterance);
            utterance.onstart = () => {
                if (state.speakGen !== myGen || state.paused) {
                    return;
                }
                state.speaking = true;
                setVoiceStatus("Trainer: " + state.title + " · beat " + (index + 1) + "/" + state.beats.length);
                syncVoiceButtons();
            };
            utterance.onend = () => {
                if (state.speakGen !== myGen || state.paused) {
                    return;
                }
                window.setTimeout(() => go(index + 1), 420);
            };
            utterance.onerror = () => {
                if (state.speakGen !== myGen || state.paused) {
                    return;
                }
                state.speaking = false;
                setVoiceStatus("Voice stopped.");
                syncVoiceButtons();
            };
            state.speaking = true;
            window.speechSynthesis.speak(utterance);
        };
        go(startIndex);
        setVoiceStatus("Trainer: " + state.title);
        syncVoiceButtons();
    }

    function speakText(title, text) {
        if (!window.speechSynthesis) {
            setVoiceStatus("This browser has no speech synthesis. Read the lesson on screen.");
            return;
        }
        stopSpeech();
        const myGen = state.speakGen;
        state.title = title;
        state.beats = splitBeats(sanitizeForVoice(text));
        state.beatIndex = 0;
        if (state.beats.length === 0) {
            setVoiceStatus("Nothing speakable — the screen still has the lesson.");
            return;
        }
        speakFrom(0, myGen);
    }

    function speakCurrent() {
        if (state.paused) {
            resumeSpeech();
            return;
        }
        speakText(state.lessonTitle, state.lessonVoice);
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;");
    }

    function paragraphs(text) {
        return String(text || "")
            .split(/\n{2,}/)
            .map((part) => part.trim())
            .filter(Boolean);
    }

    function conceptBlock(concept, kicker) {
        return `<section class="learn-block learn-${concept.kind === "release" ? "lab" : "teach"}">
            <p class="learn-kicker">${escapeHtml(kicker)}</p>
            <h3>${escapeHtml(concept.title || "")}</h3>
            <p>${escapeHtml(concept.why || "")}</p>
            ${concept.useCase ? `<p class="learn-use"><strong>In production.</strong> ${escapeHtml(concept.useCase)}</p>` : ""}
            ${concept.example ? `<pre class="preview tiny">${escapeHtml(concept.example)}</pre>` : ""}
        </section>`;
    }

    function depthBlock(concept) {
        const body = paragraphs(concept.depth || concept.why || "")
            .map((part) => `<p>${escapeHtml(part)}</p>`)
            .join("");
        return `<section class="learn-block learn-teach">
            <p class="learn-kicker">In depth</p>
            <h3>${escapeHtml(concept.title || "")}</h3>
            ${body}
            ${concept.useCase ? `<p class="learn-use"><strong>In production.</strong> ${escapeHtml(concept.useCase)}</p>` : ""}
        </section>`;
    }

    function interviewBlock(concept) {
        const cards = (concept.interviews || []).map((card) => `
            <article class="interview-card">
                <p class="q">${escapeHtml(card.question || "")}</p>
                <p>${escapeHtml(card.answer || "")}</p>
                ${card.followUp ? `<p class="interview-follow">They may also ask: ${escapeHtml(card.followUp)}</p>` : ""}
            </article>`).join("");
        return `<section class="learn-block learn-recap">
            <p class="learn-kicker">Interview</p>
            <h3>${escapeHtml(concept.title || "")}</h3>
            ${cards || "<p class=\"muted\">No interview cards for this concept yet.</p>"}
        </section>`;
    }

    function exampleBlock(concept) {
        const samples = (concept.samples || []).map((sample) => `
            <div>
                <p class="learn-kicker">${escapeHtml(sample.title || "Example")}</p>
                ${sample.notes ? `<p>${escapeHtml(sample.notes)}</p>` : ""}
                <pre class="preview tiny">${escapeHtml(sample.code || "")}</pre>
            </div>`).join("");
        const fallback = concept.example ? `<pre class="preview tiny">${escapeHtml(concept.example)}</pre>` : "";
        return `<section class="learn-block learn-lab">
            <p class="learn-kicker">Programming examples</p>
            <h3>${escapeHtml(concept.title || "")}</h3>
            <p class="hint">On the screen only. Voice will not read this.</p>
            ${samples || fallback || "<p class=\"muted\">No sample for this concept.</p>"}
        </section>`;
    }

    function ruleBlock(title, body, why, whyLabel) {
        return `<article class="rule-card">
            <h3>${escapeHtml(title)}</h3>
            <p>${escapeHtml(body)}</p>
            ${why ? `<p class="learn-use"><strong>${escapeHtml(whyLabel || "Why.")}</strong> ${escapeHtml(why)}</p>` : ""}
        </article>`;
    }

    function paneHtml(lesson, tab) {
        const concepts = lesson.concepts || [];
        const head = `<h3>${escapeHtml(lesson.name || "Skill")}</h3>`;
        if (tab === "depth") {
            return head + (concepts.map(depthBlock).join("") || "<p class=\"empty\">No depth notes.</p>");
        }
        if (tab === "interview") {
            return head + (concepts.map(interviewBlock).join("") || "<p class=\"empty\">No interview cards.</p>");
        }
        if (tab === "examples") {
            return head + (concepts.map(exampleBlock).join("") || "<p class=\"empty\">No examples.</p>");
        }
        if (tab === "standards") {
            const rules = (lesson.standards || []).map((rule) => ruleBlock(rule.title, rule.rule, rule.why)).join("");
            return head + "<p class=\"huddle-lead\">Coding standards for this skill. The generated project encodes them as StudioRules and markdown.</p>"
                + (rules || "<p class=\"empty\">No authored standards for this skill.</p>");
        }
        if (tab === "patterns") {
            const rules = (lesson.patterns || []).map((rule) => ruleBlock(rule.name, rule.intent, rule.how, "In this domain.")).join("");
            return head + "<p class=\"huddle-lead\">Design patterns this skill uses in production. Demo classes in the zip implement Strategy via ConceptDemo.</p>"
                + (rules || "<p class=\"empty\">No authored patterns for this skill.</p>");
        }
        if (tab === "project") {
            const conceptList = concepts.map((concept) => `<li>${escapeHtml(concept.title)}</li>`).join("");
            return `${head}
                <p class="huddle-lead">Generate a compiling Spring Boot studio from the basket. It includes README, coding standards, design patterns, and one demo class per selected concept.</p>
                <p>Open skill: <strong>${escapeHtml(lesson.name || "")}</strong></p>
                <ul>${conceptList || "<li>Concepts on this page</li>"}</ul>
                <p class="hint">Use Add skill / Add concept on the right, then Download project. The zip is a compiling Spring Boot studio.</p>`;
        }
        const overview = concepts.map((concept) => conceptBlock(concept, "Widely used")).join("");
        return `${head}
            <p class="huddle-lead">${escapeHtml(lesson.summary || "")}</p>
            ${overview}`;
    }

    function paintConceptChips(lesson) {
        const nav = $("conceptButtons");
        const label = $("conceptFilterLabel");
        if (!nav) {
            return;
        }
        const items = lesson.catalog || [];
        state.conceptIndex = items;
        if (!items.length) {
            nav.hidden = true;
            if (label) {
                label.hidden = true;
            }
            nav.innerHTML = "";
            return;
        }
        nav.hidden = false;
        if (label) {
            label.hidden = false;
        }
        const allOn = !state.conceptSlug;
        nav.innerHTML = `<button type="button" class="learn-chip${allOn ? " active" : ""}" data-concept="" aria-pressed="${allOn ? "true" : "false"}">All concepts</button>`
            + items.map((item) => {
                const on = item.slug === state.conceptSlug;
                return `<button type="button" class="learn-chip${on ? " active" : ""}" data-concept="${escapeHtml(item.slug)}" aria-pressed="${on ? "true" : "false"}">${escapeHtml(item.title)}</button>`;
            }).join("");
    }

    function paintPager(lesson) {
        const pager = $("lessonPager");
        if (!pager) {
            return;
        }
        const total = lesson.totalPages || 1;
        const page = lesson.page || 0;
        if (state.conceptSlug || total <= 1) {
            pager.hidden = true;
            pager.innerHTML = "";
            return;
        }
        pager.hidden = false;
        const prevDisabled = page <= 0 ? "disabled" : "";
        const nextDisabled = page >= total - 1 ? "disabled" : "";
        pager.innerHTML = `
            <button type="button" class="btn btn-outline-dark btn-sm" id="pagerPrev" ${prevDisabled}>Prev</button>
            <span class="pager-status">page ${page + 1} of ${total}</span>
            <button type="button" class="btn btn-outline-dark btn-sm" id="pagerNext" ${nextDisabled}>Next</button>
        `;
    }

    function markStudioTab(tab) {
        document.querySelectorAll(".studio-tab").forEach((button) => {
            const on = button.getAttribute("data-tab") === tab;
            button.classList.toggle("active", on);
            button.setAttribute("aria-pressed", on ? "true" : "false");
        });
        const tabs = $("studioTabs");
        if (tabs) {
            tabs.hidden = false;
        }
    }

    function renderBasket() {
        const list = $("studioBasketList");
        const status = $("studioBasketStatus");
        const download = $("studioDownload");
        if (!list) {
            return;
        }
        const skills = Object.entries(state.basket.skills);
        const concepts = Object.entries(state.basket.concepts);
        list.innerHTML = skills.map(([key, name]) =>
            `<li><span>Skill · ${escapeHtml(name)}</span><button type="button" class="btn btn-sm btn-outline-secondary" data-remove-skill="${escapeHtml(key)}">Remove</button></li>`
        ).join("") + concepts.map(([id, title]) =>
            `<li><span>Concept · ${escapeHtml(title)}</span><button type="button" class="btn btn-sm btn-outline-secondary" data-remove-concept="${escapeHtml(id)}">Remove</button></li>`
        ).join("");
        const empty = skills.length === 0 && concepts.length === 0;
        if (status) {
            status.textContent = empty
                ? (state.skillKey
                    ? "Basket empty — Download still uses the open skill."
                    : "Basket is empty.")
                : `${skills.length} skill${skills.length === 1 ? "" : "s"}, ${concepts.length} concept${concepts.length === 1 ? "" : "s"}.`;
        }
        if (download) {
            download.disabled = empty && !state.skillKey;
        }
        const addSkill = $("studioAddSkill");
        if (addSkill) {
            addSkill.disabled = !state.skillKey;
            addSkill.textContent = state.skillKey && state.basket.skills[state.skillKey] ? "Skill added" : "Add skill";
        }
        const addConcept = $("studioAddConcept");
        if (addConcept) {
            const hasFocus = Boolean(state.skillKey && state.conceptSlug);
            addConcept.disabled = !hasFocus;
            const id = hasFocus ? state.skillKey + "/" + state.conceptSlug : "";
            addConcept.textContent = id && state.basket.concepts[id] ? "Concept added" : "Add concept";
        }
    }

    function basketParams() {
        const skillKeys = Object.keys(state.basket.skills);
        const conceptIds = Object.keys(state.basket.concepts);
        const concepts = conceptIds.map((id) => id.split("/")[1] || id);
        if (!skillKeys.length && state.skillKey) {
            skillKeys.push(state.skillKey);
        }
        if (!concepts.length && state.conceptSlug) {
            concepts.push(state.conceptSlug);
        }
        const extraSkills = conceptIds.map((id) => id.split("/")[0]).filter(Boolean);
        extraSkills.forEach((key) => {
            if (!skillKeys.includes(key)) {
                skillKeys.push(key);
            }
        });
        return {
            skills: skillKeys.join(","),
            concepts: concepts.join(",")
        };
    }

    function renderSkillLesson(lesson) {
        const host = $("skillLesson");
        if (!host || !lesson) {
            return;
        }
        state.lesson = lesson;
        state.skillKey = lesson.key || "";
        state.skillName = lesson.name || "Skill";
        state.lessonTitle = lesson.name || "Skill";
        state.lessonVoice = lesson.voice || "";
        state.page = lesson.page || 0;
        paintConceptChips(lesson);
        paintPager(lesson);
        markStudioTab(state.tab || "overview");
        host.innerHTML = paneHtml(lesson, state.tab || "overview");
        syncVoiceButtons();
        const focus = state.conceptSlug
            ? (lesson.concepts && lesson.concepts[0] && lesson.concepts[0].title) || "concept"
            : lesson.name;
        setVoiceStatus(focus + " ready. Use the tabs for depth, interview, and examples. Voice skips code.");
        document.querySelectorAll(".skill-chip").forEach((chip) => {
            const on = chip.getAttribute("data-skill") === lesson.key;
            chip.classList.toggle("active", on);
            chip.setAttribute("aria-pressed", on ? "true" : "false");
        });
        renderBasket();
    }

    function paintSkillButtons(skills) {
        const nav = $("skillButtons");
        if (!nav || !skills || !skills.length) {
            return;
        }
        const current = document.querySelector(".skill-chip.active")?.getAttribute("data-skill");
        nav.innerHTML = skills.map((skill) =>
            `<button type="button" class="learn-chip skill-chip${skill.key === current ? " active" : ""}" data-skill="${escapeHtml(skill.key)}" aria-pressed="${skill.key === current ? "true" : "false"}">${escapeHtml(skill.name)}</button>`
        ).join("");
    }

    function loadSkill(skill, generate, concept, page) {
        if (!skill) {
            return;
        }
        const host = $("skillLesson");
        if (host) {
            host.innerHTML = '<p class="empty">Writing the huddle…</p>';
        }
        let path;
        if (generate) {
            path = "/api/generate?skill=" + encodeURIComponent(skill);
        } else {
            path = "/api/lesson.json?skill=" + encodeURIComponent(skill);
            if (concept) {
                path += "&concept=" + encodeURIComponent(concept);
            } else {
                const p = page == null ? state.page : page;
                path += "&page=" + encodeURIComponent(p) + "&size=" + PAGE_SIZE;
            }
        }
        fetch(path)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Could not load that skill.");
                }
                return response.json();
            })
            .then((lesson) => {
                if (lesson.error) {
                    throw new Error(lesson.error);
                }
                state.conceptSlug = concept || "";
                if (!concept && lesson.page != null) {
                    state.page = lesson.page;
                }
                renderSkillLesson(lesson);
            })
            .catch((err) => {
                if (host) {
                    host.innerHTML = `<p class="flash err">${escapeHtml(err.message || "Could not load that skill.")}</p>`;
                }
            });
    }

    function loadSkills() {
        if (!$("skillButtons")) {
            return;
        }
        fetch("/api/skills.json")
            .then((response) => response.json())
            .then((data) => paintSkillButtons(data.skills || []))
            .catch(() => {});
    }

    function bindVoiceBar() {
        document.querySelectorAll(".js-voice-play").forEach((button) => {
            button.addEventListener("click", speakCurrent);
        });
        document.querySelectorAll(".js-voice-pause").forEach((button) => {
            button.addEventListener("click", () => {
                if (state.paused) {
                    resumeSpeech();
                } else {
                    pauseSpeech();
                }
            });
        });
        document.querySelectorAll(".js-voice-stop").forEach((button) => {
            button.addEventListener("click", stopSpeech);
        });
        document.querySelectorAll(".js-voice-calm").forEach((button) => {
            button.addEventListener("click", () => {
                state.rate = 0.84;
                setVoiceStatus("Calm trainer pace. Hit Play.");
            });
        });
        document.querySelectorAll(".js-voice-brisk").forEach((button) => {
            button.addEventListener("click", () => {
                state.rate = 0.96;
                setVoiceStatus("Brisk trainer pace. Hit Play.");
            });
        });
    }

    function init() {
        if (window.speechSynthesis) {
            window.speechSynthesis.getVoices();
            window.speechSynthesis.onvoiceschanged = () => window.speechSynthesis.getVoices();
        } else {
            setVoiceStatus("Voice is unavailable in this browser. Lessons still display.");
        }

        bindVoiceBar();

        $("skillButtons")?.addEventListener("click", (event) => {
            const chip = event.target.closest("[data-skill]");
            if (!chip) {
                return;
            }
            state.conceptSlug = "";
            state.page = 0;
            state.tab = "overview";
            loadSkill(chip.getAttribute("data-skill"), false, "", 0);
        });

        $("conceptButtons")?.addEventListener("click", (event) => {
            const chip = event.target.closest("[data-concept]");
            if (!chip || !state.skillKey) {
                return;
            }
            const concept = chip.getAttribute("data-concept") || "";
            state.page = 0;
            loadSkill(state.skillKey, false, concept, 0);
        });

        $("lessonPager")?.addEventListener("click", (event) => {
            const prev = event.target.closest("#pagerPrev");
            const next = event.target.closest("#pagerNext");
            if (!state.skillKey || state.conceptSlug) {
                return;
            }
            if (prev && !prev.disabled) {
                loadSkill(state.skillKey, false, "", Math.max(0, state.page - 1));
            }
            if (next && !next.disabled) {
                loadSkill(state.skillKey, false, "", state.page + 1);
            }
        });

        $("studioTabs")?.addEventListener("click", (event) => {
            const tab = event.target.closest("[data-tab]");
            if (!tab || !state.lesson) {
                return;
            }
            state.tab = tab.getAttribute("data-tab") || "overview";
            markStudioTab(state.tab);
            const host = $("skillLesson");
            if (host) {
                host.innerHTML = paneHtml(state.lesson, state.tab);
            }
        });

        $("studioAddSkill")?.addEventListener("click", () => {
            if (!state.skillKey) {
                return;
            }
            state.basket.skills[state.skillKey] = state.skillName || state.skillKey;
            renderBasket();
        });

        $("studioAddConcept")?.addEventListener("click", () => {
            if (!state.skillKey || !state.conceptSlug) {
                return;
            }
            const id = state.skillKey + "/" + state.conceptSlug;
            const title = (state.lesson && state.lesson.concepts && state.lesson.concepts[0] && state.lesson.concepts[0].title)
                || state.conceptSlug;
            state.basket.concepts[id] = (state.skillName || state.skillKey) + " / " + title;
            if (!state.basket.skills[state.skillKey]) {
                state.basket.skills[state.skillKey] = state.skillName || state.skillKey;
            }
            renderBasket();
        });

        $("studioBasketList")?.addEventListener("click", (event) => {
            const skill = event.target.closest("[data-remove-skill]");
            const concept = event.target.closest("[data-remove-concept]");
            if (skill) {
                delete state.basket.skills[skill.getAttribute("data-remove-skill")];
            }
            if (concept) {
                delete state.basket.concepts[concept.getAttribute("data-remove-concept")];
            }
            if (skill || concept) {
                renderBasket();
            }
        });

        $("studioDownload")?.addEventListener("click", () => {
            const params = basketParams();
            if (!params.skills) {
                setVoiceStatus("Add a skill to the basket first.");
                return;
            }
            let url = "/api/project.zip?skills=" + encodeURIComponent(params.skills);
            if (params.concepts) {
                url += "&concepts=" + encodeURIComponent(params.concepts);
            }
            window.location.href = url;
        });

        $("skillLearnGenerate")?.addEventListener("click", () => {
            const query = ($("skillLearnQuery")?.value || "").trim();
            if (!query) {
                setVoiceStatus("Type a skill first.");
                return;
            }
            state.conceptSlug = "";
            state.page = 0;
            state.tab = "overview";
            loadSkill(query, true, "", 0);
            window.setTimeout(loadSkills, 400);
        });

        $("skillLearnQuery")?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                $("skillLearnGenerate")?.click();
            }
        });

        window.addEventListener("beforeunload", stopSpeech);
        syncVoiceButtons();
        renderBasket();
        loadSkills();
        const first = document.querySelector(".skill-chip");
        if (first) {
            loadSkill(first.getAttribute("data-skill"), false, "", 0);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
