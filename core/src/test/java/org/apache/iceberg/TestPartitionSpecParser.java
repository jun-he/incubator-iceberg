/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iceberg;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestPartitionSpecParser extends TableTestBase {

  private int[] expectedFieldIds;

  @Parameterized.Parameters
  public static Object[][] parameters() {
    return new Object[][] {
        new Object[] { 1, new int[]{ 1000, 1001 } },
        new Object[] { 2, new int[]{ 1001, 1000 } },
    };
  }

  public TestPartitionSpecParser(int formatVersion, int[] expectedFieldIds) {
    super(formatVersion);
    this.expectedFieldIds = expectedFieldIds;
  }

  @Test
  public void testToJson() {
    String expected = "{\n" +
        "  \"spec-id\" : 0,\n" +
        "  \"fields\" : [ {\n" +
        "    \"name\" : \"data_bucket\",\n" +
        "    \"transform\" : \"bucket[16]\",\n" +
        "    \"source-id\" : 2,\n" +
        "    \"field-id\" : 1000\n" +
        "  } ]\n" +
        "}";
    Assert.assertEquals(expected, PartitionSpecParser.toJson(table.spec(), true));

    table.updatePartitionSpec().newSpec()
        .bucket("id", 8)
        .bucket("data", 16)
        .commit();

    expected = "{\n" +
        "  \"spec-id\" : 1,\n" +
        "  \"fields\" : [ {\n" +
        "    \"name\" : \"id_bucket\",\n" +
        "    \"transform\" : \"bucket[8]\",\n" +
        "    \"source-id\" : 1,\n" +
        "    \"field-id\" : " + expectedFieldIds[0] +
        "\n  }, {\n" +
        "    \"name\" : \"data_bucket\",\n" +
        "    \"transform\" : \"bucket[16]\",\n" +
        "    \"source-id\" : 2,\n" +
        "    \"field-id\" : " + expectedFieldIds[1] +
        "\n  } ]\n" +
        "}";
    Assert.assertEquals(expected, PartitionSpecParser.toJson(table.spec(), true));
  }

  @Test
  public void testFromJsonWithFieldId() {
    String specString = "{\n" +
        "  \"spec-id\" : 1,\n" +
        "  \"fields\" : [ {\n" +
        "    \"name\" : \"id_bucket\",\n" +
        "    \"transform\" : \"bucket[8]\",\n" +
        "    \"source-id\" : 1,\n" +
        "    \"field-id\" : 1001\n" +
        "  }, {\n" +
        "    \"name\" : \"data_bucket\",\n" +
        "    \"transform\" : \"bucket[16]\",\n" +
        "    \"source-id\" : 2,\n" +
        "    \"field-id\" : 1000\n" +
        "  } ]\n" +
        "}";

    PartitionSpec spec = PartitionSpecParser.fromJson(table.schema(), specString);

    Assert.assertEquals(2, spec.fields().size());
    // should be the field ids in the JSON
    Assert.assertEquals(1001, spec.fields().get(0).fieldId());
    Assert.assertEquals(1000, spec.fields().get(1).fieldId());
  }

  @Test
  public void testFromJsonWithoutFieldId() {
    String specString = "{\n" +
        "  \"spec-id\" : 1,\n" +
        "  \"fields\" : [ {\n" +
        "    \"name\" : \"id_bucket\",\n" +
        "    \"transform\" : \"bucket[8]\",\n" +
        "    \"source-id\" : 1\n" +
        "  }, {\n" +
        "    \"name\" : \"data_bucket\",\n" +
        "    \"transform\" : \"bucket[16]\",\n" +
        "    \"source-id\" : 2\n" +
        "  } ]\n" +
        "}";

    PartitionSpec spec = PartitionSpecParser.fromJson(table.schema(), specString);

    Assert.assertEquals(2, spec.fields().size());
    // should be the default assignment
    Assert.assertEquals(1000, spec.fields().get(0).fieldId());
    Assert.assertEquals(1001, spec.fields().get(1).fieldId());
  }

  @Test
  public void testTransforms() {
    for (PartitionSpec spec : PartitionSpecTestBase.SPECS) {
      Assert.assertEquals("To/from JSON should produce equal partition spec",
          spec, roundTripJSON(spec));
    }
  }

  private static PartitionSpec roundTripJSON(PartitionSpec spec) {
    return PartitionSpecParser.fromJson(PartitionSpecTestBase.SCHEMA, PartitionSpecParser.toJson(spec));
  }
}
