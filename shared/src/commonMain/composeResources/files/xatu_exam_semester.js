(async function (parseExams, semester) {
    const semesterField = '[name="semester.id"],[name="semesterId"],[name="examSemesterId"]';
    const widget = '[id^="semesterBar"][id$="Semester"]';
    const text = element => (element.textContent || element.value || "").trim();
    const school = url => /^https?:\/\/jwgl2018\.xatu\.edu\.cn$/.test(url.origin);
    const actionPath = url => url.pathname.replace(/;jsessionid=[^/]+$/i, "");
    const isExamAction = url => school(url) &&
        /^\/eams\/[\w/!-]*exam[\w/!-]*\.action$/i.test(actionPath(url)) &&
        !/!(?:save|remove|delete|update|publish|cancel|confirm|register)/i.test(actionPath(url));
    const examLink = (value, origin) => value?.match(/(?:\/eams\/)?[\w/!-]*exam[\w/!-]*\.action/ig)
        ?.map(path => new URL(path.startsWith("/") ? path : "/eams/" + path, origin)).find(isExamAction);

    function declaredExamData(page) {
        const markup = [...page.document.querySelectorAll("script:not([src])")]
            .map(element => element.textContent || "").join("\n");
        const candidates = [...markup.matchAll(
            /["']((?:\/eams\/)?[\w/!-]*exam[\w/!-]*\.action(?:\?[^"'\s]*)?)["']/ig
        )].map(match => {
            const route = match[1].replace(/&amp;/g, "&");
            return new URL(route.startsWith("/") ? route : "/eams/" + route, page.location.origin);
        }).filter(isExamAction);
        const endpoint = candidates.find(url =>
            /!(?:examTable|search|query|list)\.action$/i.test(url.pathname) && /stdExamTable/i.test(url.pathname));
        const term = markup.match(/(?:semester\.id|semesterId)["']?\s*[,=:]\s*["']?(\d+)/)?.[1];
        return { endpoint, term };
    }

    function examContext(page) {
        if (!school(page.location)) return null;
        for (const form of page.document.forms) {
            const controls = [...form.querySelectorAll('input,button,a')];
            const switcher = controls.find(control => text(control) === "切换学期");
            const field = form.querySelector(semesterField);
            const action = new URL(form.getAttribute("action") || page.location.href, page.location.href);
            if (!switcher && !(isExamAction(action) && field)) continue;
            const declaredAction = examLink(switcher?.getAttribute("onclick"), page.location.origin);
            const endpoint = declaredAction || (isExamAction(action) ? action : null);
            if (endpoint) return { page, form, field, endpoint };
        }
        if (isExamAction(page.location)) {
            return { page, form: null, field: page.document.querySelector(semesterField), endpoint: new URL(page.location.href) };
        }
        const controls = [...page.document.querySelectorAll("a,button,input")];
        const menu = controls.find(control => text(control) === "我的考试");
        if (menu) {
            const href = new URL(menu.getAttribute("href") || page.location.href, page.location.href);
            const endpoint = isExamAction(href) ? href : examLink(menu.getAttribute("onclick"), page.location.origin) ||
                examLink(menu.getAttribute("href"), page.location.origin);
            const confirmedExamPage = controls.some(control => text(control) === "切换学期") &&
                [...page.document.querySelectorAll("select")].some(control => /batch/i.test(control.name || control.id || ""));
            if (endpoint || confirmedExamPage) {
                return {
                    page, form: null, field: page.document.querySelector(semesterField),
                    endpoint: endpoint || new URL("/eams/stdExamTable.action", page.location.origin)
                };
            }
        }
        for (const frame of page.document.querySelectorAll("iframe,frame")) {
            try {
                const child = frame.contentWindow && examContext(frame.contentWindow);
                if (child) return child;
            } catch (error) {
                if (error.name !== "SecurityError") throw error;
            }
        }
        return null;
    }

    async function request(url, body) {
        const response = await fetch(url, {
            credentials: "same-origin", mode: "same-origin",
            cache: "no-store",
            signal: AbortSignal.timeout(8_000),
            ...(body ? { method: "POST", body } : {})
        });
        if (!response.ok || new URL(response.url).origin !== location.origin) throw new Error("exam request failed");
        return { location: new URL(response.url), document: new DOMParser().parseFromString(await response.text(), "text/html") };
    }

    function termKey(value) {
        const label = String(value || "").replace(/学年|学期|\s|年|第|·/g, "");
        const matched = label.match(/(\d{4})[—–-](\d{4})(秋季|春季|[12])/);
        if (!matched) return "";
        const term = matched[3] === "秋季" ? "1" : matched[3] === "春季" ? "2" : matched[3];
        return matched[1] + "-" + matched[2] + "-" + term;
    }

    function termConfirmed(page, term) {
        const fields = [...page.document.querySelectorAll(semesterField + "," + widget + ",input[readonly]")];
        const expected = termKey(term.label);
        return fields.some(field => String(field.value || "") === term.id ||
            Boolean(expected && termKey(text(field)) === expected) ||
            Boolean(expected && termKey(field.value) === expected)) || declaredExamData(page).term === term.id;
    }

    async function submit(endpoint, fields, post) {
        const target = new URL(endpoint.href);
        if (!post) target.search = fields.toString();
        return request(target, post ? fields : undefined);
    }

    async function readExamBatch(page, savedContext, term) {
        const result = parseExams(page, page !== window);
        if (result !== null) return result;
        const batch = [...page.document.querySelectorAll("select")].find(select =>
            /batch/i.test(select.name || select.id || "") ||
            [...select.options].some(option => /考试|补缓考/.test(text(option))));
        const declared = declaredExamData(page);
        const options = [...(batch?.options || [])].filter(option => /^\d+$/.test(option.value) &&
            option.value !== "0" && text(option) !== "...");
        const choice = options.find(option => option.selected) ||
            options.find(option => text(option).includes("期末考试")) || options[0];
        if (!declared.endpoint && (!batch?.name || !choice)) return null;
        const form = batch?.form;
        const formAction = form && new URL(form.getAttribute("action") || page.location.href, page.location.href);
        const endpoint = examLink(batch?.getAttribute("onchange"), page.location.origin) || declared.endpoint ||
            (formAction && isExamAction(formAction) ? formAction : null) || savedContext.endpoint;
        const fields = new URLSearchParams(endpoint.searchParams);
        if (form) {
            for (const [key, value] of new FormData(form)) {
                if (!fields.has(key)) fields.set(key, value);
            }
        }
        if (batch?.name && choice) fields.set(batch.name, choice.value);
        if (term?.id) fields.set(savedContext.field?.name || "semester.id", term.id);
        fields.set("sf_request_type", "ajax");
        const response = await submit(endpoint, fields, form?.method?.toLowerCase() === "post");
        if (term?.id && [...response.document.querySelectorAll(semesterField)].some(field =>
            /^\d+$/.test(field.value) && field.value !== "0" && field.value !== term.id)) return null;
        return parseExams(response, true);
    }

    if (!school(location)) return null;
    const context = examContext(window);
    if (!context) return null;
    if (!semester) {
        const options = [...(context.field?.options || [])].filter(option => /^\d+$/.test(option.value) && option.value !== "0");
        if (options.length > 1) return options.map(option => ({ id: option.value, label: text(option) }));
        let tag = context.page.document.querySelector(widget)?.id;
        if (!tag) {
            const course = await request("/eams/courseTableForStd.action?sf_request_type=ajax");
            tag = course.document.querySelector(widget)?.id;
        }
        if (!tag) return null;
        const calendar = await fetch("/eams/dataQuery.action?sf_request_type=ajax", {
            method: "POST", credentials: "same-origin", mode: "same-origin", redirect: "error",
            body: new URLSearchParams({ tagId: tag, dataType: "semesterCalendar" }),
            signal: AbortSignal.timeout(8_000)
        });
        if (!calendar.ok) return null;
        const response = (await calendar.text()).trim().replace(/^\((.*)\);?$/s, "$1");
        let entries;
        try {
            entries = Object.values(JSON.parse(response).semesters || {}).flat();
        } catch (_) {
            entries = [...response.matchAll(/\{([^{}]+)\}/g)].map(match => {
                const field = name => match[1].match(new RegExp(
                    "(?:^|,)\\s*[\"']?" + name + "[\"']?\\s*:\\s*[\"']?([\\d-]+)"
                ))?.[1];
                return { id: field("id"), schoolYear: field("schoolYear"), name: field("name") };
            });
        }
        const optionsFromCalendar = entries
            .filter(item => /^\d+$/.test(String(item.id)) && /^\d{4}-\d{4}$/.test(String(item.schoolYear)));
        return optionsFromCalendar.reverse().map(item => ({
            id: String(item.id), label: item.schoolYear + " 学年 · " + (String(item.name) === "1" ? "秋季学期" : "春季学期")
        }));
    }

    if (semester.id === "") {
        const current = await readExamBatch(context.page, context, null);
        if (current !== null) return current;
        const fields = new URLSearchParams(context.endpoint.searchParams);
        fields.set("sf_request_type", "ajax");
        const initial = await submit(context.endpoint, fields, false);
        return readExamBatch(initial, context, null);
    }
    if (!/^\d+$/.test(semester.id)) return null;
    const fields = new URLSearchParams(context.form ? new FormData(context.form) : context.endpoint.searchParams);
    for (const key of [...fields.keys()]) {
        if (/batch/i.test(key)) fields.delete(key);
    }
    const termFieldName = context.field?.name || "semester.id";
    fields.set(termFieldName, semester.id);
    fields.set("sf_request_type", "ajax");
    const endpoint = context.endpoint;
    const post = context.form?.method?.toLowerCase() === "post";
    const result = await submit(endpoint, fields, post);
    if (!termConfirmed(result, semester)) return null;
    return readExamBatch(result, context, semester);
})
