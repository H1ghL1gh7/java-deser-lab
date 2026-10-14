package com.lab.deser;

import java.io.Serializable;

/**
 * 业务类样例（白名单放行）
 */
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String owner;
    private long amount;

    public Voucher() {}

    public Voucher(String id, String owner, long amount) {
        this.id = id;
        this.owner = owner;
        this.amount = amount;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public long getAmount() { return amount; }

    @Override
    public String toString() {
        return "Voucher{" + id + "," + owner + "," + amount + "}";
    }
}
