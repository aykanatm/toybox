package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "external_shares")
public class ExternalShare {
    @Id
    @Column(name = "external_share_id")
    @JsonProperty("externalShareId")
    private String externalShareId;

    @Column(name = "username")
    @JsonProperty("username")
    private String username;

    @Column(name = "job_id")
    @JsonProperty("jobId")
    private long jobId;

    @Column(name = "expiration_date")
    @JsonProperty("expirationDate")
    private Date expirationDate;

    @Column(name = "max_number_of_hits")
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;
}
