package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

internal object XatuImport {
    const val loginUrl =
        "https://authserver.xatu.edu.cn/authserver/login?service=https%3A%2F%2Fehall.xatu.edu.cn%2Flogin%3Fservice%3Dhttps%3A%2F%2Fehall.xatu.edu.cn%2Fnew%2Findex.html"
    const val adapterPath = "XATU/myschool.js"
    const val examParserAsset = "xatu_exams.js"
    const val examHomeUrl = "http://jwgl2018.xatu.edu.cn/eams/homeExt.action"
    const val openAcademicService = """
        (function () {
            if (location.origin !== "https://ehall.xatu.edu.cn") return false;
            const matches = element => element.getClientRects().length &&
                /^(教务管理信息系统|教务系统)$/.test((element.innerText || element.textContent).trim());
            const service = [...document.querySelectorAll("a,button,[role='button']")].find(matches) ||
                [...document.querySelectorAll("span,div")].find(element => !element.children.length && matches(element));
            if (!service) return false;
            service.click();
            return true;
        })();
    """
    private val signInPage = Regex("""^https://authserver\.xatu\.edu\.cn/authserver/login(?:[?#].*)?$""")
    private val portalPage = Regex("""^https://ehall\.xatu\.edu\.cn(?:/[^?#]*)?(?:[?#].*)?$""")

    private val coursePage = Regex(
        """^https?://jwgl2018\.xatu\.edu\.cn(/eams(?:/[^?#]*)?)(?:[?#].*)?$""",
        RegexOption.IGNORE_CASE
    )
    private val authenticationPath = Regex("""^/eams/(?:login|logout|cas|sso)""", RegexOption.IGNORE_CASE)

    private fun isAcademicLogin(url: String): Boolean =
        coursePage.matchEntire(url)?.groupValues?.get(1)?.let(authenticationPath::containsMatchIn) == true

    fun requiresSchoolSignIn(url: String) = signInPage.matches(url) || isAcademicLogin(url)

    fun isPortalPage(url: String) = portalPage.matches(url)

    fun examEntryRedirect(url: String, triedPortalSignIn: Boolean): String? = when {
        !triedPortalSignIn && requiresSchoolSignIn(url) -> loginUrl
        url.substringBefore('#').substringBefore('?') == "https://ehall.xatu.edu.cn/new/index.html" -> examHomeUrl
        else -> null
    }

    const val academicSessionReady = """
        (function () {
            if (location.hostname !== "jwgl2018.xatu.edu.cn") return false;
            return [...document.querySelectorAll("a,button,[role='menuitem']")].some(element =>
                /^(我的课表|我的考试)$/.test((element.innerText || element.textContent).trim()));
        })();
    """

    fun acceptsImportFrom(url: String): Boolean {
        val path = coursePage.matchEntire(url)?.groupValues?.get(1) ?: return false
        return !authenticationPath.containsMatchIn(path)
    }
}
