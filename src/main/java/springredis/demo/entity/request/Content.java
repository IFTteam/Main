package springredis.demo.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @JsonProperty("html")
    private String html;

    public void setHtml(String content, String link) {
        if (content.contains("http")) {
            int index = content.indexOf("http");
            String plainText = content.substring(0, index);
            String hyperlink = content.substring(index);

            this.html = String.format("%s<a href='%s'>%s</a><br><br><a href='%s'>Unsubscribe</a>", plainText, hyperlink, hyperlink, link);
        } else {
            this.html = String.format("<a href='%s'>Hi</a><br><br><a href='%s'>Unsubscribe</a>", content, link);
        }
    }

    @JsonProperty("text")
    private String text;

    @JsonProperty("content")
    private String content;

    @Override
    public String toString() {
        return "Content{" +
                "sender=" + sender +
                ", subject='" + subject + '\'' +
                ", html='" + html + '\'' +
                ", text='" + text + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
