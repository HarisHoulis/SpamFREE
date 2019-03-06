package xoulis.xaris.com.spamfree.view.codes

import android.view.View
import xoulis.xaris.com.spamfree.data.vo.ClientCode

interface CodeListener {

    fun onCodeLongClick(view:View, codeId: String)

    fun onCodeChangeMessageLimitClick(code: ClientCode)

    fun onCodeChangeDateClick(code: ClientCode)
}