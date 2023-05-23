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

    @Override
    public String toString() {
        return "Content{" +
                "sender=" + sender +
                ", subject='" + subject + '\'' +
                ", html='" + html + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
