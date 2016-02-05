/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.factory('bdmBridgeDomainService', function(VPPRestangular) {
        var s = {};

        var BridgeDomain = function(topologyId) {
            this['topology-id'] = topologyId || null;
            this['topology-types'] = {
                'vbridge-topology:vbridge-topology': {}
            };
            this['underlay-topology'] = [
                {
                    'topology-ref': 'topology-netconf'
                }
            ];
            this['vbridge-topology:tunnel-type'] = 'tunnel-type-vxlan';
            this['vbridge-topology:vxlan'] = {
                'vni': '1'
            };
            this['vbridge-topology:flood'] = "true",
            this['vbridge-topology:forward'] = "true",
            this['vbridge-topology:learn'] = "true",
            this['vbridge-topology:unknown-unicast-flood'] = "true",
            this['vbridge-topology:arp-termination'] = "false"
        };

        s.createObj = function(topologyId) {
            return new BridgeDomain(topologyId);
        };

        s.add = function(bridgeDomain, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomain['topology-id']);
            var dataObj = {'topology': [bridgeDomain]};

            restObj.customPUT(dataObj).then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.get = function(successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology');

            restObj.get().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };


        return s;
    });
});