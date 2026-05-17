package com.invman.inbound;

import com.invman.common.connector.InboundConnector;
import com.invman.common.connector.PosInvoicePayload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EscPosConnector implements InboundConnector {

    @Override
    public String getType() {
        return "ESCPOS";
    }

    @Override
    public List<PosInvoicePayload> fetchInvoices(String configJson) {
        throw new UnsupportedOperationException("ESC/POS connector not yet implemented");
    }
}
