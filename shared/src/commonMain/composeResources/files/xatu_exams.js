(function (page = window, detached = false) {
    const dateHeaders = ["考试日期", "考试安排日期", "日期"];
    const timeHeaders = ["考试时间", "考试安排", "考试时段", "考试时间安排", "考试起止时间", "时间"];
    const roomHeaders = ["考试地点", "考试教室", "考试场地", "考场地点", "考场", "教室", "地点"];
    const detailHeaders = new Set([
        "考试类别", "考试类型", "考试形式", "考核方式", "校区", "考试校区",
        "考场座号", "考试座号", "座位号", "座号", "考试状态", "考试情况", "备注", "其它说明", "其他说明"
    ]);

    function cellText(cell) {
        return (cell.innerText ?? cell.textContent ?? "").replace(/\s+/g, " ").trim();
    }

    function field(headings, values, names) {
        for (const name of names) {
            const value = values[headings.indexOf(name)];
            if (value) return value;
        }
        return "";
    }

    function readTable(table) {
        let headings = [];
        let hasConfirmedEmptyRow = false;
        const carried = [];
        const exams = [];
        for (const row of table.rows) {
            if (row.hidden || row.style?.display === "none" || (!detached && !row.getClientRects().length)) continue;
            const cells = Array.from(row.cells);
            const values = cells.map(cellText);
            const grid = [];
            for (let index = 0; index < carried.length; index++) {
                if (carried[index]?.remaining > 0) {
                    grid[index] = carried[index].value;
                    carried[index].remaining--;
                }
            }
            let column = 0;
            cells.forEach((cell, index) => {
                while (grid[column] !== undefined) column++;
                const width = Math.max(1, cell.colSpan || 1);
                for (let offset = 0; offset < width; offset++, column++) {
                    grid[column] = offset === 0 ? values[index] : "";
                    if (cell.rowSpan > 1) carried[column] = { value: grid[column], remaining: cell.rowSpan - 1 };
                }
            });
            const labels = grid.map(value => value.replace(/\s+/g, "").replace(/[：:]+$/, ""));
            const hasScheduleColumns = labels.some(label => [...dateHeaders, ...timeHeaders, ...roomHeaders].includes(label)) &&
                labels.some(label => label.startsWith("考试") || label === "考场");
            if (labels.some(label => ["课程名称", "考试科目", "考试名称", "科目名称", "课程"].includes(label)) &&
                hasScheduleColumns) {
                headings = labels;
                continue;
            }
            if (!headings.length) continue;
            if (values.length === 1 && /^(?:暂无(?:考试安排|考试|记录|数据)|没有(?:考试安排|考试|记录|数据)|无考试安排|没有找到(?:任何)?(?:记录|数据))[。！!。]?$/.test(values[0])) {
                hasConfirmedEmptyRow = true;
                continue;
            }
            if (grid.length !== headings.length) continue;
            const course = field(headings, grid, ["课程名称", "考试科目", "考试名称", "科目名称", "课程"]);
            if (!course || /^(合计|总计)$/.test(course)) continue;
            const available = value => /^(?:[~～\-—–]+|无|时间未安排|地点未安排|时间未发布|地点未发布)$/.test(value) ? "" : value;
            const date = available(field(headings, grid, dateHeaders));
            const time = available(field(headings, grid, timeHeaders));
            const room = available(field(headings, grid, roomHeaders));
            const details = headings.flatMap((label, index) =>
                detailHeaders.has(label) && grid[index] ? [{ label, value: grid[index] }] : []);
            exams.push({ course, date, time, room, details });
        }
        return exams.length || hasConfirmedEmptyRow ? exams : null;
    }

    function readPage(page) {
        if (!/^https?:$/.test(page.location.protocol) ||
            page.location.hostname !== "jwgl2018.xatu.edu.cn" ||
            !/^\/eams(?:\/|$)/i.test(page.location.pathname) ||
            /^\/eams\/(?:login|logout|cas|sso)/i.test(page.location.pathname)) return null;
        const results = [...page.document.querySelectorAll("table")]
            .filter(table => !table.hidden && table.style?.display !== "none" &&
                (detached || table.getClientRects().length))
            .map(readTable)
            .filter(result => result !== null);
        for (const frame of page.document.querySelectorAll("iframe,frame")) {
            if (!frame.getClientRects().length || !frame.contentWindow) continue;
            try {
                const result = readPage(frame.contentWindow);
                if (result !== null) results.push(result);
            } catch (error) {
                if (error.name !== "SecurityError") throw error;
            }
        }
        return results.length ? results.flat() : null;
    }

    return readPage(page);
})
