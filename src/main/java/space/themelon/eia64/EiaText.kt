package space.themelon.eia64

object EiaText {
    const val RESET: String = "\u001b[0m"
    const val BLACK: String = "\u001b[0;30m"
    const val RED: String = "\u001b[0;31m"
    const val GREEN: String = "\u001b[0;32m"
    const val YELLOW: String = "\u001b[0;33m"
    const val BLUE: String = "\u001b[0;34m"
    const val PURPLE: String = "\u001b[0;35m"
    const val CYAN: String = "\u001b[0;36m"
    const val WHITE: String = "\u001b[0;37m"
    const val BOLD: String = "\u001b[1m"
    const val UNDERLINE: String = "\u001b[4m"

    const val BLACK_BG = "\u001B[40m"
    const val RED_BG = "\u001B[41m"
    const val GREEN_BG = "\u001B[42m"
    const val YELLOW_BG = "\u001B[43m"
    const val BLUE_BG = "\u001B[44m"
    const val PURPLE_BG = "\u001B[45m"
    const val CYAN_BG = "\u001B[46m"
    const val WHITE_BG = "\u001B[47m"


    var INTRO =
        "\t\t${BOLD}❄\uFE0FWelcome to Eia!$RESET\n\n" +
                "\t${YELLOW}${BOLD}Git ${CYAN}${BOLD} ${UNDERLINE}github.com/XomaDev/Eia64$RESET\n" +
                "\t${RED}${BOLD}Docs ${CYAN}${UNDERLINE}themelon.space/eia$RESET\n\n" +
                "\t${PURPLE}${BOLD}Created with love ♥\uFE0F by Kumaraswamy B G$RESET\n" +
                BLUE + BOLD +
                """
              ___                   ___
             /\  \        ___      /\  \
            /::\  \      /\  \    /::\  \
           /:/\:\  \     \:\  \  /:/\:\  \
          /::\~\:\  \    /::\__\/::\~\:\  \
         /:/\:\ \:\__\__/:/\/__/:/\:\ \:\__\
         \:\~\:\ \/__/\/:/  /  \/__\:\/:/  /
          \:\ \:\__\ \::/__/        \::/  /
           \:\ \/__/  \:\__\        /:/  /
            \:\__\     \/__/       /:/  /
             \/__/                 \/__/

             """ + "$RESET\n\n"

    val SHELL_STYLE = "$RESET$BLUE_BG eia \$ $RESET $BLUE".toByteArray()
}
