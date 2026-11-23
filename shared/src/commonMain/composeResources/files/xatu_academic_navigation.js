(function (menu, openMenu, runQuery) {
    function inspect(page) {
        if (!/^https?:$/.test(page.location.protocol) ||
            page.location.hostname !== "jwgl2018.xatu.edu.cn" ||
            !/^\/eams(?:\/|$)/i.test(page.location.pathname) ||
            /^\/eams\/(?:login|logout|cas|sso)/i.test(page.location.pathname)) return "waiting";
        const doc = page.document;
        const label = element => (element.innerText || element.value || element.textContent || "").trim();
        const visible = element => element.getClientRects().length > 0;
        if (doc.querySelector('input[type="password"]')) return "login";
        if (openMenu) {
            const link = [...doc.querySelectorAll('a, button, [role="menuitem"]')].find(element =>
                visible(element) && label(element) === menu);
            if (link) {
                link.click();
                return "opened";
            }
        }
        const pathMatches = menu === "我的成绩" ? /grade/i : /exam/i;
        if (runQuery && pathMatches.test(page.location.pathname)) {
            const query = [...doc.querySelectorAll('button,input[type="submit"],input[type="button"],a')].find(element =>
                visible(element) && !element.disabled && /^(查询|搜索)$/.test(label(element)));
            if (query) {
                query.click();
                return "queried";
            }
        }
        for (const frame of doc.querySelectorAll("iframe,frame")) {
            try {
                if (!visible(frame) || !frame.contentWindow) continue;
                const result = inspect(frame.contentWindow);
                if (result !== "waiting") return result;
            } catch (error) {
                if (error.name !== "SecurityError") throw error;
            }
        }
        return "waiting";
    }
    return inspect(window);
})
