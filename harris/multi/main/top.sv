module top(
    input   logic       clk, reset,
    output  logic       mem_enab,
    output  logic[31:0] write_data, data_addr
);

    ctrl_bus_if ctrl_bus();
    assign ctrl_bus.clk     = clk;
    assign ctrl_bus.reset   = reset;

    mother_board    mother_board(.*);
    
endmodule