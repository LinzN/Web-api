/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.restfulapi.api.jsonapi;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class RequestData {
    private List<String> subChannels;
    private Map<String, String> postQueryData;
    private InetSocketAddress inetSocketAddress;

    public RequestData(List<String> subChannels, Map<String, String> postQueryData, InetSocketAddress inetSocketAddress) {
        this.subChannels = subChannels;
        this.postQueryData = postQueryData;
        this.inetSocketAddress = inetSocketAddress;
    }

    public List<String> getSubChannels() {
        return subChannels;
    }

    public Map<String, String> getPostQueryData() {
        return postQueryData;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }
}
