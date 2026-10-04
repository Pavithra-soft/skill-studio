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
        basket: { skills: {}, concepts: {} },
        conversationId: "studio-" + Math.random().toString(36).slice(2, 10),
        lastTutorVoice: "",
        pendingQuiz: false,
        llm: false
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
        return Boolean(state.lesson);
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
        if (state.tab === "standards") {
            speakText((state.skillName || "Skill") + " standards", standardsScript(state.lesson));
            return;
        }
        if (state.tab === "patterns") {
            speakText((state.skillName || "Skill") + " patterns", patternsScript(state.lesson));
            return;
        }
        speakText(state.lessonTitle, pageScript(state.lesson, visibleConcepts()));
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;");
    }

    function conceptTabs() {
        return state.tab !== "standards" && state.tab !== "patterns" && state.tab !== "project";
    }

    function totalPages() {
        const n = ((state.lesson && state.lesson.concepts) || []).length;
        return Math.max(1, Math.ceil(n / PAGE_SIZE));
    }

    function pageBounds() {
        const n = ((state.lesson && state.lesson.concepts) || []).length;
        const from = n === 0 ? 0 : state.page * PAGE_SIZE + 1;
        const to = Math.min(n, state.page * PAGE_SIZE + PAGE_SIZE);
        return { from, to, n };
    }

    function visibleConcepts() {
        const all = (state.lesson && state.lesson.concepts) || [];
        if (state.conceptSlug) {
            return all.filter((concept) => concept.slug === state.conceptSlug);
        }
        const start = state.page * PAGE_SIZE;
        return all.slice(start, start + PAGE_SIZE);
    }

    function clampPage() {
        const max = totalPages() - 1;
        if (state.page > max) {
            state.page = Math.max(0, max);
        }
        if (state.page < 0) {
            state.page = 0;
        }
    }

    function pageScript(lesson, concepts) {
        if (!lesson) {
            return "";
        }
        const bits = ["Alright. This is a working session on " + (lesson.name || "this skill") + "."];
        const bounds = pageBounds();
        if (!state.conceptSlug && bounds.n > PAGE_SIZE) {
            bits.push("Concepts " + bounds.from + " to " + bounds.to + " of " + bounds.n + ".");
        }
        if (lesson.summary && state.page === 0 && !state.conceptSlug) {
            bits.push(lesson.summary);
        }
        (concepts || []).forEach((concept) => {
            bits.push(concept.title + ".");
            (concept.points || []).slice(0, 6).forEach((point) => bits.push(point));
            if (!(concept.points && concept.points.length) && (concept.depth || concept.why)) {
                bits.push(concept.depth || concept.why || "");
            }
            if (concept.trap) {
                bits.push("Watch-out. " + concept.trap);
            }
            if (concept.takeaway) {
                bits.push("Takeaway. " + concept.takeaway);
            }
            if (concept.useCase) {
                bits.push("In production. " + concept.useCase);
            }
            (concept.interviews || []).slice(0, 1).forEach((card) => {
                bits.push("Interview question. " + card.question);
                if (card.answer) {
                    bits.push(card.answer);
                }
            });
            if ((concept.samples && concept.samples.length) || concept.example) {
                bits.push("There are programming examples on the screen. I will not read the code.");
            }
        });
        bits.push("That is this page. Pause me any time.");
        return bits.filter(Boolean).join(" ");
    }

    function standardsScript(lesson) {
        if (!lesson) {
            return "";
        }
        const bits = ["Coding standards for " + (lesson.name || "this skill") + "."];
        (lesson.standards || []).forEach((rule) => {
            bits.push(rule.title + ".");
            bits.push(rule.rule || "");
            if (rule.why) {
                bits.push("Why. " + rule.why);
            }
        });
        return bits.filter(Boolean).join(" ");
    }

    function patternsScript(lesson) {
        if (!lesson) {
            return "";
        }
        const bits = ["Design patterns for " + (lesson.name || "this skill") + "."];
        (lesson.patterns || []).forEach((rule) => {
            bits.push(rule.name + ".");
            bits.push(rule.intent || "");
            if (rule.how) {
                bits.push("In this domain. " + rule.how);
            }
        });
        return bits.filter(Boolean).join(" ");
    }

    function syncUrl() {
        const params = new URLSearchParams();
        if (state.skillKey) {
            params.set("skill", state.skillKey);
        }
        if (state.conceptSlug) {
            params.set("concept", state.conceptSlug);
        } else if (state.page > 0) {
            params.set("page", String(state.page + 1));
        }
        if (state.tab && state.tab !== "overview") {
            params.set("tab", state.tab);
        }
        const query = params.toString();
        const next = query ? ("/?" + query) : "/";
        if (window.location.pathname + window.location.search !== next) {
            history.replaceState(null, "", next);
        }
    }

    function readUrl() {
        const params = new URLSearchParams(window.location.search);
        const pageRaw = parseInt(params.get("page") || "1", 10);
        return {
            skill: params.get("skill") || "",
            concept: params.get("concept") || "",
            tab: params.get("tab") || "overview",
            page: Number.isFinite(pageRaw) ? Math.max(0, pageRaw - 1) : 0
        };
    }

    function paragraphs(text) {
        return String(text || "")
            .split(/\n{2,}/)
            .map((part) => part.trim())
            .filter(Boolean);
    }

    function conceptBlock(concept, kicker) {
        const points = (concept.points || []).map((point) => `<li>${escapeHtml(point)}</li>`).join("");
        const generated = concept.generated ? '<span class="generated-pill">Generated</span>' : "";
        return `<section class="learn-block learn-${concept.kind === "release" ? "lab" : "teach"}">
            <p class="learn-kicker">${escapeHtml(kicker)}</p>
            <h3>${escapeHtml(concept.title || "")}${generated}</h3>
            <p>${escapeHtml(concept.why || "")}</p>
            ${points ? `<ol class="lesson-points">${points}</ol>` : ""}
            ${concept.useCase ? `<p class="learn-use"><strong>In production.</strong> ${escapeHtml(concept.useCase)}</p>` : ""}
            ${concept.trap ? `<p class="trap-line"><strong>Watch-out.</strong> ${escapeHtml(concept.trap)}</p>` : ""}
            ${concept.takeaway ? `<p class="takeaway-line"><strong>Takeaway.</strong> ${escapeHtml(concept.takeaway)}</p>` : ""}
            ${concept.example ? `<pre class="preview tiny">${escapeHtml(concept.example)}</pre>` : ""}
        </section>`;
    }

    function depthBlock(concept) {
        const paras = paragraphs(concept.depth || "")
            .map((part, index) => `<p><strong>${index + 1}.</strong> ${escapeHtml(part)}</p>`)
            .join("");
        const fallback = !paras && (concept.why || "")
            ? `<p>${escapeHtml(concept.why)}</p>`
            : paras;
        const points = (concept.points || []).map((point) => `<li>${escapeHtml(point)}</li>`).join("");
        return `<section class="learn-block learn-teach">
            <p class="learn-kicker">In depth</p>
            <h3>${escapeHtml(concept.title || "")}</h3>
            ${points ? `<ol class="lesson-points">${points}</ol>` : ""}
            ${fallback}
            ${concept.trap ? `<p class="trap-line"><strong>Watch-out.</strong> ${escapeHtml(concept.trap)}</p>` : ""}
            ${concept.takeaway ? `<p class="takeaway-line"><strong>Takeaway.</strong> ${escapeHtml(concept.takeaway)}</p>` : ""}
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
        const concepts = visibleConcepts();
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
            return "";
        }
        if (tab === "patterns") {
            const rules = (lesson.patterns || []).map((rule) => ruleBlock(rule.name, rule.intent, rule.how, "In this domain.")).join("");
            return head + "<p class=\"huddle-lead\">Design patterns this skill uses in production. Demo classes in the zip implement Strategy via ConceptDemo.</p>"
                + (rules || "<p class=\"empty\">No authored patterns for this skill.</p>");
        }
        if (tab === "project") {
            const conceptList = (lesson.concepts || []).map((concept) => `<li>${escapeHtml(concept.title)}</li>`).join("");
            return `${head}
                <p class="huddle-lead">Generate a compiling Spring Boot studio from the basket. It includes README, coding standards, design patterns, and one demo class per selected concept.</p>
                <p>Open skill: <strong>${escapeHtml(lesson.name || "")}</strong></p>
                <ul>${conceptList || "<li>Concepts on this skill</li>"}</ul>
                <p class="hint">Use Add skill / Add concept on the right, then Download project. The zip is a compiling Spring Boot studio.</p>`;
        }
        const overview = concepts.map((concept) => conceptBlock(concept, "Widely used")).join("");
        const bounds = pageBounds();
        const pageNote = !state.conceptSlug && bounds.n > PAGE_SIZE
            ? `<p class="hint">Showing concepts ${bounds.from}–${bounds.to} of ${bounds.n}. Use the pager for the next set.</p>`
            : "";
        return `${head}
            <p class="huddle-lead">${escapeHtml(lesson.summary || "")}</p>
            ${pageNote}
            ${overview}`;
    }

    function paintConceptChips(lesson) {
        const nav = $("conceptButtons");
        const label = $("conceptFilterLabel");
        if (!nav) {
            return;
        }
        const items = lesson.catalog && lesson.catalog.length
            ? lesson.catalog
            : (lesson.concepts || []).map((concept) => ({ slug: concept.slug, title: concept.title }));
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
            const bounds = pageBounds();
            label.textContent = state.conceptSlug
                ? "One concept"
                : (bounds.n > PAGE_SIZE
                    ? ("Filter by concept · ring means this page (" + bounds.from + "–" + bounds.to + " of " + bounds.n + ")")
                    : "Filter by concept");
        }
        const allOn = !state.conceptSlug;
        const onPage = new Set(visibleConcepts().map((concept) => concept.slug));
        nav.innerHTML = `<button type="button" class="learn-chip${allOn ? " active" : ""}" data-concept="" aria-pressed="${allOn ? "true" : "false"}">All concepts</button>`
            + items.map((item) => {
                const on = item.slug === state.conceptSlug;
                const pageMark = allOn && onPage.has(item.slug) ? " on-page" : "";
                return `<button type="button" class="learn-chip${on ? " active" : ""}${pageMark}" data-concept="${escapeHtml(item.slug)}" aria-pressed="${on ? "true" : "false"}">${escapeHtml(item.title)}</button>`;
            }).join("");
    }

    function paintPager() {
        const pager = $("lessonPager");
        if (!pager) {
            return;
        }
        const total = totalPages();
        if (state.conceptSlug || total <= 1 || !conceptTabs()) {
            pager.hidden = true;
            pager.innerHTML = "";
            return;
        }
        pager.hidden = false;
        const bounds = pageBounds();
        let nums = "";
        for (let i = 0; i < total; i++) {
            nums += `<button type="button" class="pager-num${i === state.page ? " active" : ""}" data-page="${i}"${i === state.page ? " aria-current=\"page\"" : ""}>${i + 1}</button>`;
        }
        pager.innerHTML = `
            <button type="button" class="btn btn-outline-dark btn-sm" data-pager="prev"${state.page <= 0 ? " disabled" : ""}>Prev</button>
            <div class="pager-nums">${nums}</div>
            <button type="button" class="btn btn-outline-dark btn-sm" data-pager="next"${state.page >= total - 1 ? " disabled" : ""}>Next</button>
            <span class="pager-status">Concepts ${bounds.from}–${bounds.to} of ${bounds.n}</span>
        `;
    }

    function paintStandards() {
        const host = $("skillStandards");
        if (!host || !state.lesson) {
            return;
        }
        const rules = state.lesson.standards || [];
        const expanded = state.tab === "standards";
        if (!rules.length || state.tab === "project") {
            host.hidden = true;
            return;
        }
        host.hidden = false;
        host.classList.toggle("expanded", expanded);
        const heading = $("skillStandardsHeading");
        if (heading) {
            heading.textContent = "Coding standards · " + (state.lesson.name || "Skill");
        }
        const lead = $("skillStandardsLead");
        if (lead) {
            lead.textContent = expanded
                ? "These rules apply to the whole skill. Concept paging does not change them."
                : "These rules belong to the skill, not to a concept page. Paging below does not change them.";
        }
        const open = $("skillStandardsOpen");
        if (open) {
            open.hidden = expanded;
        }
        const body = $("skillStandardsBody");
        if (body) {
            body.innerHTML = rules.map((rule, index) => `
                <article class="rule-card">
                    <p class="learn-kicker">Standard ${index + 1} of ${rules.length}</p>
                    <h3>${escapeHtml(rule.title || "")}</h3>
                    <p>${escapeHtml(rule.rule || "")}</p>
                    ${rule.why ? `<p class="learn-use"><strong>Why.</strong> ${escapeHtml(rule.why)}</p>` : ""}
                </article>`).join("");
        }
        const count = $("standardsCount");
        if (count) {
            count.textContent = String(rules.length);
        }
        const patternsCount = $("patternsCount");
        if (patternsCount) {
            patternsCount.textContent = String((state.lesson.patterns || []).length);
        }
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
        const genConcept = $("conceptLearnGenerate");
        if (genConcept) {
            genConcept.disabled = !(state.skillKey && state.conceptSlug);
        }
        syncTutor();
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
        clampPage();
        paintConceptChips(lesson);
        paintPager();
        paintStandards();
        markStudioTab(state.tab || "overview");
        const hideArticle = state.tab === "standards";
        host.hidden = hideArticle;
        if (!hideArticle) {
            host.innerHTML = paneHtml(lesson, state.tab || "overview");
        }
        syncVoiceButtons();
        const bounds = pageBounds();
        if (state.tab === "standards") {
            setVoiceStatus("Coding standards for " + (lesson.name || "this skill") + " are on screen. Play reads the rules, not the code.");
        } else if (state.conceptSlug) {
            const title = (visibleConcepts()[0] && visibleConcepts()[0].title) || "concept";
            setVoiceStatus(title + " ready. Use the tabs for depth, interview, and examples.");
        } else if (bounds.n > PAGE_SIZE && conceptTabs()) {
            setVoiceStatus((lesson.name || "Skill") + " · concepts " + bounds.from + "–" + bounds.to + " of " + bounds.n + ".");
        } else {
            setVoiceStatus((lesson.name || "Skill") + " ready. Coding standards stay visible above the huddle.");
        }
        document.querySelectorAll(".skill-chip").forEach((chip) => {
            const on = chip.getAttribute("data-skill") === lesson.key;
            chip.classList.toggle("active", on);
            chip.setAttribute("aria-pressed", on ? "true" : "false");
        });
        renderBasket();
        syncUrl();
        syncTutor();
    }

    function goPage(page) {
        if (!state.lesson || state.conceptSlug) {
            return;
        }
        const next = Math.max(0, Math.min(page, totalPages() - 1));
        if (next === state.page) {
            return;
        }
        state.page = next;
        renderSkillLesson(state.lesson);
        $("skillLesson")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function paintSkillButtons(skills) {
        const nav = $("skillButtons");
        if (!nav) {
            return;
        }
        if (!skills) {
            skills = [];
        }
        const current = state.skillKey || document.querySelector(".skill-chip.active")?.getAttribute("data-skill");
        nav.innerHTML = skills.map((skill) =>
            `<span class="skill-chip-wrap">
                <button type="button" class="learn-chip skill-chip${skill.key === current ? " active" : ""}" data-skill="${escapeHtml(skill.key)}" aria-pressed="${skill.key === current ? "true" : "false"}">${escapeHtml(skill.name)}</button>
                <button type="button" class="skill-delete" data-delete-skill="${escapeHtml(skill.key)}" title="Remove skill" aria-label="Remove ${escapeHtml(skill.name)}">×</button>
            </span>`
        ).join("");
    }

    function loadSkill(skill, generate, concept) {
        if (!skill) {
            return;
        }
        const host = $("skillLesson");
        if (host) {
            host.hidden = false;
            host.innerHTML = '<p class="empty">' + (generate && concept ? "Writing a tutor card…" : "Writing the huddle…") + "</p>";
        }
        let path = generate
            ? "/api/generate?skill=" + encodeURIComponent(skill)
            : "/api/lesson.json?skill=" + encodeURIComponent(skill);
        if (generate && concept) {
            path += "&concept=" + encodeURIComponent(concept);
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
                if (generate && concept) {
                    const wanted = concept;
                    const fromConcepts = (lesson.concepts || []).find((item) => item.slug === wanted || item.title === wanted);
                    const fromCatalog = (lesson.catalog || []).find((item) => item.slug === wanted);
                    state.conceptSlug = (fromConcepts && fromConcepts.slug) || (fromCatalog && fromCatalog.slug) || wanted;
                } else {
                    state.conceptSlug = concept || "";
                    if (state.conceptSlug) {
                        const exists = (lesson.concepts || []).some((item) => item.slug === state.conceptSlug)
                            || (lesson.catalog || []).some((item) => item.slug === state.conceptSlug);
                        if (!exists) {
                            state.conceptSlug = "";
                        }
                    }
                }
                clampPage();
                renderSkillLesson(lesson);
                if (generate && concept) {
                    setVoiceStatus("Tutor card ready. Ask the chatbot, or Play trainer voice.");
                }
            })
            .catch((err) => {
                if (host) {
                    host.hidden = false;
                    host.innerHTML = `<p class="flash err">${escapeHtml(err.message || "Could not load that skill.")}</p>`;
                }
            });
    }

    function applySkillsPayload(data) {
        paintSkillButtons((data && data.skills) || []);
        state.llm = Boolean(data && data.llm);
        const status = $("llmStatus");
        if (status && data) {
            status.textContent = data.llm
                ? ("Tutor model · " + (data.provider || "gemini"))
                : "Tutor search uses the catalog. Set GEMINI_API_KEY to let ChatClient rewrite cards.";
        }
    }

    function loadSkills() {
        if (!$("skillButtons")) {
            return;
        }
        fetch("/api/skills.json")
            .then((response) => response.json())
            .then(applySkillsPayload)
            .catch(() => {});
    }

    function deleteSkill(key) {
        if (!key) {
            return;
        }
        fetch("/api/skill/delete?skill=" + encodeURIComponent(key))
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Could not remove that skill.");
                }
                return response.json();
            })
            .then((data) => {
                applySkillsPayload(data);
                delete state.basket.skills[key];
                Object.keys(state.basket.concepts).forEach((id) => {
                    if (id.startsWith(key + "/")) {
                        delete state.basket.concepts[id];
                    }
                });
                if (state.skillKey === key) {
                    state.conceptSlug = "";
                    state.page = 0;
                    state.tab = "overview";
                    state.lesson = null;
                    state.skillKey = "";
                    state.skillName = "";
                    const first = document.querySelector(".skill-chip");
                    if (first) {
                        loadSkill(first.getAttribute("data-skill"), false, "");
                    } else {
                        const host = $("skillLesson");
                        if (host) {
                            host.innerHTML = '<p class="empty">Pick a skill button to open the huddle.</p>';
                        }
                        syncTutor();
                        renderBasket();
                    }
                } else {
                    renderBasket();
                }
            })
            .catch((err) => setVoiceStatus(err.message || "Could not remove that skill."));
    }

    function syncTutor() {
        const ready = Boolean(state.skillKey);
        const ask = $("tutorAsk");
        const quiz = $("tutorQuiz");
        const speak = $("tutorSpeak");
        if (ask) {
            ask.disabled = !ready;
        }
        if (quiz) {
            quiz.disabled = !ready;
        }
        if (speak) {
            speak.disabled = !state.lastTutorVoice;
        }
        const hint = $("tutorHint");
        if (hint && ready) {
            if (state.pendingQuiz) {
                hint.textContent = "Quiz is open. Type your answer and hit Ask — I will score it against the catalog.";
            } else if (state.conceptSlug) {
                hint.textContent = "I answer from this concept first, then the rest of the catalog.";
            } else {
                hint.textContent = "Ask a concrete question. I retrieve the best catalog card, then answer in bullets.";
            }
        }
        const status = $("tutorStatus");
        if (status && ready && status.textContent === "Pick a skill to chat.") {
            status.textContent = "Ask in text. Speak last answer reads it aloud.";
        }
    }

    function appendTutor(role, text) {
        const log = $("tutorLog");
        if (!log) {
            return;
        }
        const div = document.createElement("div");
        div.className = "tutor-bubble " + role;
        div.textContent = text;
        log.appendChild(div);
        log.scrollTop = log.scrollHeight;
    }

    function askTutor(quiz) {
        const input = $("tutorInput");
        const message = quiz ? "" : ((input && input.value) || "").trim();
        const status = $("tutorStatus");
        if (!state.skillKey) {
            if (status) {
                status.textContent = "Pick a skill to chat.";
            }
            return;
        }
        if (!quiz && !message) {
            if (status) {
                status.textContent = "Type a question first.";
            }
            return;
        }
        if (!quiz && input) {
            appendTutor("user", message);
            input.value = "";
        }
        if (status) {
            status.textContent = quiz
                ? "Picking a quiz question…"
                : (state.pendingQuiz ? "Scoring your answer…" : "Finding the best catalog card…");
        }
        fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                skill: state.skillKey,
                concept: state.conceptSlug || "",
                message,
                conversationId: state.conversationId,
                quiz: Boolean(quiz)
            })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Tutor could not answer.");
                }
                return response.json();
            })
            .then((reply) => {
                const answer = reply.answer || "";
                const voice = reply.voice || answer;
                state.lastTutorVoice = voice;
                state.pendingQuiz = /^\s*Quiz\./i.test(answer);
                appendTutor("bot", answer);
                const src = (reply.sources || []).filter(Boolean).slice(0, 2).join(" · ");
                if (status) {
                    status.textContent = (reply.llm ? "Model · " : "Catalog · ")
                        + (src || reply.provider || "catalog");
                }
                syncTutor();
                if (quiz && voice) {
                    speakText("Quiz", voice);
                }
            })
            .catch((err) => {
                if (status) {
                    status.textContent = err.message || "Tutor could not answer.";
                }
            });
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
            const del = event.target.closest("[data-delete-skill]");
            if (del) {
                event.preventDefault();
                event.stopPropagation();
                deleteSkill(del.getAttribute("data-delete-skill"));
                return;
            }
            const chip = event.target.closest("[data-skill]");
            if (!chip) {
                return;
            }
            state.conceptSlug = "";
            state.page = 0;
            state.tab = "overview";
            loadSkill(chip.getAttribute("data-skill"), false, "");
        });

        $("conceptButtons")?.addEventListener("click", (event) => {
            const chip = event.target.closest("[data-concept]");
            if (!chip || !state.lesson) {
                return;
            }
            const concept = chip.getAttribute("data-concept") || "";
            state.conceptSlug = concept;
            if (concept) {
                const index = (state.lesson.concepts || []).findIndex((item) => item.slug === concept);
                if (index >= 0) {
                    state.page = Math.floor(index / PAGE_SIZE);
                }
                if (state.tab === "standards" || state.tab === "patterns" || state.tab === "project") {
                    state.tab = "overview";
                }
            }
            renderSkillLesson(state.lesson);
        });

        $("lessonPager")?.addEventListener("click", (event) => {
            const prev = event.target.closest("[data-pager=\"prev\"]");
            const next = event.target.closest("[data-pager=\"next\"]");
            const num = event.target.closest("[data-page]");
            if (!state.skillKey || state.conceptSlug) {
                return;
            }
            if (prev && !prev.disabled) {
                goPage(state.page - 1);
            }
            if (next && !next.disabled) {
                goPage(state.page + 1);
            }
            if (num) {
                goPage(Number(num.getAttribute("data-page")));
            }
        });

        $("studioTabs")?.addEventListener("click", (event) => {
            const tab = event.target.closest("[data-tab]");
            if (!tab || !state.lesson) {
                return;
            }
            state.tab = tab.getAttribute("data-tab") || "overview";
            renderSkillLesson(state.lesson);
        });

        $("skillStandardsOpen")?.addEventListener("click", () => {
            if (!state.lesson) {
                return;
            }
            state.tab = "standards";
            renderSkillLesson(state.lesson);
            $("skillStandards")?.scrollIntoView({ behavior: "smooth", block: "start" });
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
            const match = ((state.lesson && state.lesson.concepts) || []).find((item) => item.slug === state.conceptSlug);
            const title = (match && match.title) || state.conceptSlug;
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
                if (state.skillKey && state.conceptSlug) {
                    loadSkill(state.skillKey, true, state.conceptSlug);
                    return;
                }
                setVoiceStatus("Type a skill first, or select a concept to generate.");
                return;
            }
            state.conceptSlug = "";
            state.page = 0;
            state.tab = "overview";
            loadSkill(query, true, "");
            window.setTimeout(loadSkills, 400);
        });

        $("conceptLearnGenerate")?.addEventListener("click", () => {
            if (!state.skillKey || !state.conceptSlug) {
                setVoiceStatus("Select a skill and a concept first.");
                return;
            }
            loadSkill(state.skillKey, true, state.conceptSlug);
        });

        $("tutorAsk")?.addEventListener("click", () => askTutor(false));
        $("tutorQuiz")?.addEventListener("click", () => askTutor(true));
        $("tutorSpeak")?.addEventListener("click", () => {
            if (!state.lastTutorVoice) {
                return;
            }
            speakText("Tutor", state.lastTutorVoice);
        });
        $("tutorInput")?.addEventListener("keydown", (event) => {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                askTutor(false);
            }
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
        const fromUrl = readUrl();
        if (fromUrl.tab) {
            state.tab = fromUrl.tab;
        }
        if (fromUrl.page) {
            state.page = fromUrl.page;
        }
        if (fromUrl.concept) {
            state.conceptSlug = fromUrl.concept;
        }
        const first = document.querySelector(".skill-chip");
        const startSkill = fromUrl.skill || (first && first.getAttribute("data-skill")) || "";
        if (startSkill) {
            loadSkill(startSkill, false, fromUrl.concept);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
