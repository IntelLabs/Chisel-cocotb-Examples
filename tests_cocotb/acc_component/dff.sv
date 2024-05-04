// This file is public domain, it can be freely copied without restrictions.
// SPDX-License-Identifier: CC0-1.0

// Adapted from https://github.com/themperek/cocotb-test


module dff_test (
  input logic clk,
  input logic reset,
  output logic [7:0] q
);

always @(posedge clk) begin
  if (reset) begin
     q <= 0;
  end else begin
     q <= q + 1;
  end
end

endmodule
