package com.invman.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.PosInvoicePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PosEventMapperTest {

    private PosEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PosEventMapper(new ObjectMapper());
    }

    @Test
    void mapR2OResponse_validJson_parsedCorrectly() {
        String json = """
                [
                  {
                    "document_id": "INV-100",
                    "invoiceitems": [
                      { "product_id": 10001, "invoiceitem_amount": 3.0 },
                      { "product_id": 10002, "invoiceitem_amount": 1.0 }
                    ]
                  }
                ]
                """;

        List<PosInvoicePayload> result = mapper.mapR2OResponse(json, "READY2ORDER");

        assertThat(result).hasSize(1);
        PosInvoicePayload invoice = result.get(0);
        assertThat(invoice.invoiceId()).isEqualTo("INV-100");
        assertThat(invoice.posSource()).isEqualTo("READY2ORDER");
        assertThat(invoice.items()).hasSize(2);
        assertThat(invoice.items().get(0).productId()).isEqualTo("10001");
        assertThat(invoice.items().get(0).quantitySold()).isEqualTo(3.0);
    }

    @Test
    void mapR2OResponse_productIdZero_skipped() {
        String json = """
                [
                  {
                    "document_id": "INV-101",
                    "invoiceitems": [
                      { "product_id": 0, "invoiceitem_amount": 2.0 },
                      { "product_id": 10001, "invoiceitem_amount": 1.0 }
                    ]
                  }
                ]
                """;

        List<PosInvoicePayload> result = mapper.mapR2OResponse(json, "READY2ORDER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).items()).hasSize(1);
        assertThat(result.get(0).items().get(0).productId()).isEqualTo("10001");
    }

    @Test
    void mapR2OResponse_malformedJson_returnsEmptyList() {
        String json = "{ this is not valid json }}}";

        List<PosInvoicePayload> result = mapper.mapR2OResponse(json, "READY2ORDER");

        assertThat(result).isEmpty();
    }
}
