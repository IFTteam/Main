package springredis.demo.entity.response;

import lombok.Getter;

@Getter
public class AnalyticsResponse {
    String friendly_from;
    String subject;
    String sending_domain;
    String type;
    String raw_rcpt_to;
    String transmission_id;
    String campaign_id;
    String timestamp;
    String ip_address;
    long customer_id;
    String injection_time;

    @Override
    public String toString() {
        return "friendly_from: "+ this.friendly_from+" "+
                "subject: "+ this.subject+" "+
                "sending_domain: "+ this.sending_domain+" "+
                "type: "+ this.type+" "+
                "raw_rcpt_to: "+ this.raw_rcpt_to+" "+
                "transmission_id: "+ this.transmission_id+" "+
                "campaign_id: "+ this.campaign_id+" "+
                "timestamp: "+ this.timestamp+" "+
                "subject: "+ this.subject+" "+
                "ip_address: "+ this.ip_address+" "+
                "customer_id: "+ this.customer_id+" "+
                "injection_time: "+ this.injection_time+" ";
    }
}