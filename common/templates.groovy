import groovy.text.SimpleTemplateEngine

def mailTemplate(valueMap, inputID, question) {

    def t = '''
<html xmlns="http://www.w3.org/1999/xhtml">
   <head>
      <meta http-equiv="content-type" content="text/html; charset=utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0;">
      <meta name="format-detection" content="telephone=no"/>
      <style>
         /* Reset styles */ 
         body { margin: 0; padding: 0; min-width: 100%; width: 100% !important; height: 100% !important;}
         body, table, td, div, p, a { -webkit-font-smoothing: antialiased; text-size-adjust: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%; line-height: 100%; }
         table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse !important; border-spacing: 0; }
         img { border: 0; line-height: 100%; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }
         #outlook a { padding: 0; }
         .ReadMsgBody { width: 100%; } .ExternalClass { width: 100%; }
         .ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div { line-height: 100%; }
         /* Rounded corners for advanced mail clients only */ 
         @media all and (min-width: 560px) {
         .container { border-radius: 8px; -webkit-border-radius: 8px; -moz-border-radius: 8px; -khtml-border-radius: 8px;}
         }
         /* Set color for auto links (addresses, dates, etc.) */ 
         a, a:hover {
         color: #127DB3;
         }
         .footer a, .footer a:hover {
         color: #999999;
         }
      </style>
      <title>EXUS - CI</title>
   </head>
   <body topmargin="0" rightmargin="0" bottommargin="0" leftmargin="0" marginwidth="0" marginheight="0" width="100%" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; width: 100%; height: 100%; -webkit-font-smoothing: antialiased; text-size-adjust: 100%; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%; line-height: 100%;
      background-color: #F0F0F0;
      color: #000000;"
      bgcolor="#F0F0F0"
      text="#000000">
      <table width="100%" align="center" border="0" cellpadding="0" cellspacing="0" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; width: 100%;" class="background">
         <tr>
            <td align="center" valign="top" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0;"
               bgcolor="#F0F0F0">
               <table border="0" cellpadding="0" cellspacing="0" align="center"
                  width="560" style="border-collapse: collapse; border-spacing: 0; padding: 0; width: inherit;
                  max-width: 560px;" class="wrapper">
                  <tr>
                     <td align="center" valign="top" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%;
                        padding-top: 20px;
                        padding-bottom: 20px;">
                        <img border="0" vspace="0" hspace="0"
                           src="http://mingle.exus.co.uk/Images/EXUS_CORAL_RGB.jpg"
                           width="120" 
                           alt="EXUS Web & Mobile" title="EXUS Web & style" Mobile="
                           color: #000000;
                           font-size: 10px; margin: 0; padding: 0; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; border: none; display: block;" />
                     </td>
                  </tr>
               </table>
               <table border="0" cellpadding="0" cellspacing="0" align="center"
                  bgcolor="#FFFFFF"
                  width="560" style="border-collapse: collapse; border-spacing: 0; padding: 0; width: inherit;
                  max-width: 560px;" class="container">
                  <tr>
                     <td align="center" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 24px; font-weight: bold; line-height: 130%;
                        padding-top: 25px;
                        color: #000000;
                        font-family: sans-serif;" class="header">
                        $fullVersion
                     </td>
                  </tr>
                  <tr>
                     <td align="left" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 17px; font-weight: 400; line-height: 160%;
                        padding-top: 25px; 
                        color: #000000;font-family: sans-serif;" class="paragraph">
                        <strong>App Name:</strong> $appName<br>
                        <strong>OS Name:</strong> $oSName<br>
                        <strong>Release State:</strong> $releaseState<br>
                     </td>
                  </tr>
                  <tr >
                     <td align="center" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 17px; font-weight: 400; line-height: 160%;
                        padding-top: 25px; 
                        color: #000000;
                        font-family: sans-serif; padding-bottom:15px;" class="paragraph">
                        <strong>$question</strong> 
                     </td>
                  </tr>
                  <tr>
                     <td>
                        <table border="0" cellspacing="0" cellpadding="0">
                           <tr>
                              <td align="center" style="-webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px;width:320px;" bgcolor="#4CAF50">
                                 <a href="$proceedURL" target="_blank" style="font-size: 16px; font-family: Helvetica, Arial, sans-serif; color: #ffffff; text-decoration: none; text-decoration: none; -webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; padding: 30px 18px; border: 1px solid #2F6732; display: inline-block;width:320px;">OK &rarr;</a>
                              </td>
                              <td align="center" style="-webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px;width:160px" bgcolor="#F44336">
                                 <a href="$abortURL" target="_blank" style="font-size: 16px; font-family: Helvetica, Arial, sans-serif; color: #ffffff; text-decoration: none; text-decoration: none; -webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; padding: 30px 18px; border: 1px solid #BB0F02; display: inline-block;width:160px">Cancel &#10005;</a>
                              </td>
                           </tr>
                        </table>
                     </td>
                  </tr>
                  <tr>
                     <td align="center" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%;
                        padding-top: 25px;" class="line">
                        <hr
                           color="#E0E0E0" align="center" width="100%" size="1" noshade style="margin: 0; padding: 0;" />
                     </td>
                  </tr>
                  <tr>
                     <td align="center" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 18px; font-weight: bold; line-height: 130%;
                        padding-top: 25px;
                        color: #000000;
                        font-family: sans-serif;" class="header">
                        Git Log
                     </td>
                  </tr>
                  <tr>
                     <td align="center" valign="top" colspan="2" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 17px; font-weight: 400; line-height: 160%;
                        padding-top: 20px;
                        padding-bottom: 25px;
                        color: #000000;
                        font-family: sans-serif;" class="paragraph">
                        $gitLogSubjects
                     </td>
                  </tr>
               </table>
               <table border="0" cellpadding="0" cellspacing="0" align="center"
                  width="560" style="border-collapse: collapse; border-spacing: 0; padding: 0; width: inherit;
                  max-width: 560px;" class="wrapper">
                  <tr>
                     <td align="center" valign="top" style="border-collapse: collapse; border-spacing: 0; margin: 0; padding: 0; padding-left: 6.25%; padding-right: 6.25%; width: 87.5%; font-size: 13px; font-weight: 400; line-height: 150%;
                        padding-top: 20px;
                        padding-bottom: 20px;
                        color: #999999;
                        font-family: sans-serif;" class="footer">
                        This email was sent to&nbsp;you because we&nbsp;want to&nbsp;make the&nbsp;world a&nbsp;better place. </a>
                     </td>
                  </tr>
               </table>
            </td>
         </tr>
      </table>
   </body>
</html>

	'''

    def model = [
            appName       : valueMap['appName'],
            oSName        : valueMap['oSName'],
            releaseState  : valueMap['releaseState'],
            fullVersion   : valueMap['fullVersion'],
            gitLogSubjects: valueMap['gitLogSubjects'],
            question      : question,
            proceedURL    : valueMap['actionURL'] + inputID + "/" + "proceedEmpty",
            abortURL      : valueMap['actionURL'] + inputID + "/" + "abort"
    ]


    String result = new SimpleTemplateEngine().createTemplate(t).make(model).toString()

    return result
}

return this;