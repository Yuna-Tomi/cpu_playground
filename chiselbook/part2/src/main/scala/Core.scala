package fetch

import chisel3._
import chisel3.util._
import common.Consts._
import common.Instructions._

class Core extends Module {
	val io = IO(new Bundle {
		val imem = Flipped(new ImemPortIo())
		val dmem = Flipped(new DmemPortIo())
		val exit = Output(Bool())
	})

	val regfile = Mem(32, UInt(WORD_LEN.W))

    // Fetch
    /* -------------------------------------------------- */
	val pc_reg = RegInit(START_ADDR)
	val pc_plus4 = pc_reg + 4.U(WORD_LEN.W)
	io.imem.addr := pc_reg
	val inst = io.imem.inst
    /* -------------------------------------------------- */


    // Decode
    /* -------------------------------------------------- */
    val rs1_addr    = inst(19, 15)
    val rs2_addr    = inst(24, 20)
    val rd_addr     = inst(11, 7)
    val rs1_data    = Mux(rs1_addr =/= 0.U(WORD_LEN.W), regfile(rs1_addr), 0.U(WORD_LEN.W));
    val rs2_data    = Mux(rs2_addr =/= 0.U(WORD_LEN.W), regfile(rs2_addr), 0.U(WORD_LEN.W));

    val imm_i       = inst(31, 20)
    val imm_i_sext  = Cat(Fill(20, imm_i(11)), imm_i)
    val imm_s       = Cat(inst(31, 25), inst(11, 7))
    val imm_s_sext  = Cat(Fill(20, imm_s(11)), imm_s)
    val imm_b       = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8))
    val imm_b_sext  = Cat(Fill(19, imm_b(12)), imm_b)
    val imm_j       = Cat(inst(31), inst(19, 12), inst(29), inst(30, 21))
    val imm_j_sext  = Cat(Fill(11, imm_j(19)), imm_j, 0.U)

    val imm_u       = inst(31, 12)
    val imm_u_sft   = Cat(imm_u, Fill(20, 0.U))
    val imm_z       = inst(19, 15)
    val imm_z_uext  = Cat(Fill(27, 0.U), imm_z)

    val ctrl_sig = ListLookup(
        inst, List(ALU_X, OP1_RS1, OP2_RS2, M_EN_X, R_EN_X, WB_X, CSR_X),
        Array(
            LW		-> List(ALU_ADD, OP1_RS1, OP2_IMI, M_EN_X, R_EN_S, WB_MEM, CSR_X),
            SW		-> List(ALU_ADD, OP1_RS1, OP2_RS2, M_EN_S, R_EN_X, WB_X, CSR_X),

            ADD		-> List(ALU_ADD, OP1_RS1, OP2_RS2, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            ADDI	-> List(ALU_ADD, OP1_RS1, OP2_IMI, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            SUB		-> List(ALU_SUB, OP1_RS1, OP2_RS2, M_EN_X, R_EN_S, WB_ALU, CSR_X),

            AND		-> List(ALU_AND, OP1_RS1, OP2_RS2, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            OR		-> List(ALU_OR , OP1_RS1, OP2_RS2, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            XOR		-> List(ALU_XOR, OP1_RS1, OP2_RS2, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            ANDI	-> List(ALU_AND, OP1_RS1, OP2_IMI, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            ORI		-> List(ALU_OR , OP1_RS1, OP2_IMI, M_EN_X, R_EN_S, WB_ALU, CSR_X),
            XORI	-> List(ALU_XOR, OP1_RS1, OP2_IMI, M_EN_X, R_EN_S, WB_ALU, CSR_X),

            // SLL		-> List(ALU_SLL, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SRL		-> List(ALU_SRL, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SRA		-> List(ALU_SRA, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SLLI	-> List(ALU_SLL, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SRLI	-> List(ALU_SRL, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SRAI	-> List(ALU_SRA, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),

            // SLT		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SLTU	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SLTI	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // SLTIU	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),

            // BEQ		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // BNE		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // BLT		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // BGE		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // BLTU	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // BGEU	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),

            // JAL		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // JALR	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),

            // LUI		-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
            // AUIPC	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),

            // CSRRW	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),
            // CSRRWI	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),
            // CSRRS	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),
            // CSRRSI	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),
            // CSRRC	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),
            // CSRRCI	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_),

            // ECALL	-> List(ALU_, OP1_, OP2_, M_EN_, R_EN_, WB_, CSR_X),
        )
    )

    val alu_sig :: op1_sel :: op2_sel :: mem_en :: reg_en :: wb_sel :: csr_cmd :: Nil = ctrl_sig;
    /* -------------------------------------------------- */

    // Execute
    /* -------------------------------------------------- */
    val op1_data = MuxCase(0.U(WORD_LEN.W), Seq(
        (op1_sel === OP1_RS1) -> rs1_data,
    ));
    val op2_data = MuxCase(0.U(WORD_LEN.W), Seq(
        (op2_sel === OP2_RS2) -> rs2_data,
        (op2_sel === OP2_IMI) -> imm_i_sext,
        (op2_sel === OP2_IMS) -> imm_s_sext,
    ));

    // ALU
    val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
        (alu_sig === ALU_ADD) -> (op1_data + op2_data),
        (alu_sig === ALU_SUB) -> (op1_data - op2_data),
        (alu_sig === ALU_AND) -> (op1_data & op2_data),
        (alu_sig === ALU_OR)  -> (op1_data | op2_data),
        (alu_sig === ALU_XOR) -> (op1_data ^ op2_data),
    ));
    /* -------------------------------------------------- */


    // Memory
    /* -------------------------------------------------- */
    io.dmem.addr := alu_out
    /* -------------------------------------------------- */

    // WriteBack
    /* -------------------------------------------------- */
    val wb_data = MuxCase(alu_out, Seq(
        (wb_sel === WB_MEM) -> (io.dmem.rdata),
        (wb_sel === WB_PC)  -> (pc_plus4),
    )) 

    when(reg_en === R_EN_S) {
        regfile(rd_addr) := wb_data
    }
    
    /* -------------------------------------------------- */
	io.exit := (inst === 0x34333231.U(WORD_LEN.W))

	printf(p"pc_reg	: 0x${Hexadecimal(pc_reg)}\n")
	printf(p"inst	: 0x${Hexadecimal(inst)}\n")
	printf("-------------------\n")
}

