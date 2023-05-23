package springredis.demo.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.regex.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Content {
    @JsonProperty("from")
    private Sender sender;

    @JsonProperty("subject")
    private String subject;
    private String link;

    private String link1; // Added variable for link1
    private String link2; // Added variable for link2

    @JsonProperty("html")
    private String html;

    public void setHtml(String link1, String link2) {
        this.link1 = link1; // Set the value of link1
        this.link2 = link2; // Set the value of link2
        this.html = "<a href='" + link1 + "'>CNN news</a><br><a href='" + link2 + "'>Unsubscribe</a>";
    }

    public String getLink1() {
        return link1;
    }

    public String getLink2() {
        return link2;
    }

    @JsonProperty("text")
    private String text;


    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private String content;

    public void setHtml(String content, String link) {
//        String regex = "(?i)(http|https)://[^\\s<>\"']+";
//        Pattern pattern = Pattern.compile(regex);
        //String regex = "(?i)(http|https)://\\S+";
        String regex = "^*((https|http|ftp|rtsp|mms)?://)"
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?"
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}"
                + "|"
                + "([0-9a-z_!~*'()-]+\\.)*"
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."
                + "[a-z]{2,6})"
                + "(:[0-9]{1,4})?"
                + "((/?)|"
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)*$";
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(content);

        StringBuilder htmlBuilder = new StringBuilder();

        int lastIndex = 0;
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            //String plainText = content.substring(lastIndex, startIndex);
            String hyperlink = content.substring(startIndex, endIndex);
            htmlBuilder.append(content, lastIndex, startIndex);
            //htmlBuilder.append(plainText);
            htmlBuilder.append("<a href='");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("'>");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("</a><br><br>");

            lastIndex = endIndex;
        }

        htmlBuilder.append(content.substring(lastIndex));
        htmlBuilder.append("<br><br><a href='");
        htmlBuilder.append(link);
        htmlBuilder.append("'>Unsubscribe</a>");

        this.html = htmlBuilder.toString();
    }

    public void setText(String content, String link) {

        // Append the plain text content
        this.html = "<span>" +
                escapeHtml(content) +
                "</span>" +

                // Append the unsubscribe link
                "<br><br><a data-msys-unsubscribe=\"1\" href=\"" +
                link +
                "\">Unsubscribe</a>";
    }


    private String escapeHtml(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public String toString() {
        return "Content{" +
                "sender=" + sender +
                ", subject='" + subject + '\'' +
                ", html='" + html + '\'' +
                ", text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}