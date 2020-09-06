package br.com.flow.prod.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class flow_rel_inst_evento_VerificaPedido implements EventoProgramavelJava {
	
	private String tarefa = "UserTask_0k0pho2";//produção
	//private String tarefa = "UserTask_1r6c4w6";//teste
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();	
		BigDecimal nroUnico = VO.asBigDecimal("NUNOTA");
		BigDecimal nroProcesso = VO.asBigDecimal("IDINSTPRN");
		
		//Se o pedido for igual a zero ele deixa, se for maior que zero ele valida se é um núemro único válido.
		if(nroUnico.intValue()==0) {
			VO.setProperty("OBSERVACAO", "NÃO É NECESSARIO GERAR PEDIDOS");
		}else if(nroUnico.intValue()!=0) {
			
			if(!validaNroUnico(nroUnico)) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O número único "+nroUnico+" não existe no sistema!!</b></font><br/><br/><br/>\n\n\n");
			}else {
				
				DynamicVO tgfcab = getTGFCAB(nroUnico);
				Timestamp dataNegociacao = tgfcab.asTimestamp("DTNEG");
				
				if(dataNegociacao==null) {
					dataNegociacao = new Timestamp(System.currentTimeMillis());
				}
				
				/*
				 * if(!getDataCriacao(nroProcesso,dataNegociacao)) { throw new
				 * PersistenceException(
				 * "<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/>"
				 * + "\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>A data do pedido "
				 * +nroUnico+" está menor que a data da criação da tarefa, verificar se este é o número único correto!!</b></font><br/><br/><br/>\n\n\n"
				 * ); }
				 */
			}
			
		}
		
	}
	
	private boolean validaNroUnico(BigDecimal nunota) throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	private boolean getDataCriacao(BigDecimal idprocesso, Timestamp dataNegociacao) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("InstanciaTarefa");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND IDELEMENTO=?",new Object[] { idprocesso,tarefa });	
		
		Timestamp dataCriacao = VO.asTimestamp("DHCRIACAO");
			
		if(dataCriacao.after(dataNegociacao)) {
			valida = false;
		}else if (dataCriacao.before(dataNegociacao)) {
			valida = true;
		}else {
			valida = true;
		}
		
		return valida;
	}
	
}
