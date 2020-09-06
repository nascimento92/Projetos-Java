package br.com.TCIBEM;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_ajustaLote implements AcaoRotinaJava {
	
	String retorno = "";
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
		arg0.setMensagemRetorno(retorno);
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		String empresa = (String) arg0.getParam("CODEMP");
		String lote = (String) arg0.getParam("LOTE");
		String local = (String) arg0.getParam("CODLOCAL");
		String parceiro = (String) arg0.getParam("CODPARC");
		String tipo = (String) arg0.getParam("TIPO");
		String produto = (String) arg0.getParam("CODPROD");
		String status = (String) arg0.getParam("STATUS");
		
		if("1".equals(tipo)) {
			
			if(!validaLote(lote)) {
				arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
			"<b>LOTE INFORMADO NÃO É VÁLIDO! <br/> O LOTE DEVE REPRESENTAR ALGUM PATRIMONIO DO SISTEMA!</b><br/>");
			}
			
			if(empresa==null) {
				arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"<b>Para a opção Ajustar/Cadastrar, a empresa deve ser informada!</b><br/>");
			}
			
			excluirLote(lote);
			cadastrarLote(lote,empresa,local,parceiro);
			
			if(status!=null) {
				excluiStatusLote(lote);
				cadastraStatusLote(lote,status,empresa);
			}
			
			this.retorno="Lote: "+lote+" ajustado / cadastrado!";
			geraLog(lote);
		}else if("2".equals(tipo)) {
			excluirLote(lote);
			this.retorno="Lote: "+lote+" excluido!";
			geraLog(lote);
		}else {
			
			if(produto==null) {
				arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"<b>Para excluir lotes vazios o produto deve ser informado!</b><br/>");
			}
			
			if(empresa==null) {
				arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"<b>Para excluir lotes vazios a empresa deve ser informada!</b><br/>");
			}
			
			excluirLote(" ",empresa,produto);
			this.retorno="Lote Vazio excluido!";
			geraLog("LOTE VAZIO");
		}

	}
	
	private boolean validaLote(Object lote) throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { lote });
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	private void excluirLote(String lote) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("Estoque", "this.CONTROLE=?",new Object[] {lote}));
			
		} catch (Exception e) {
			System.out.println("## [br.tcibem.btn_ajustaLote] ## - Nao foi possivel excluir o lote!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void excluirLote(String lote, String empresa, String produto) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("Estoque", "this.CONTROLE=? AND this.CODEMP=? AND this.CODPROD=?",new Object[] {lote,empresa,produto}));
			
		} catch (Exception e) {
			System.out.println("## [br.tcibem.btn_ajustaLote] ## - Nao foi possivel excluir o lote!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void cadastrarLote(String lote, String empresa, String local, String parceiro) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Estoque");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODEMP", new BigDecimal(empresa));
			if(local!=null) {
				VO.setProperty("CODLOCAL",  new BigDecimal(local));
			}else {
				VO.setProperty("CODLOCAL", new BigDecimal(1500));
			}
			VO.setProperty("CODPROD", getTCIBEM(lote).asBigDecimal("CODPROD"));
			VO.setProperty("CONTROLE", lote);
			VO.setProperty("RESERVADO", new BigDecimal(0));
			VO.setProperty("ESTMIN", new BigDecimal(0));
			VO.setProperty("ESTMAX", new BigDecimal(0));
			VO.setProperty("ATIVO", "S");
			VO.setProperty("TIPO", "P");
			
			if(parceiro!=null) {
				VO.setProperty("CODPARC",  new BigDecimal(parceiro));
			}else {
				VO.setProperty("CODPARC", new BigDecimal(0));
			}
			
			VO.setProperty("STATUSLOTE", "N");
			VO.setProperty("ESTOQUE", new BigDecimal(1));
			
			dwfFacade.createEntity("Estoque", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [br.tcibem.btn_ajustaLote] ## - Nao foi possivel Cadastrar o lote!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private DynamicVO getTCIBEM(Object lote) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { lote });
		return VO;
	}
	
	private void geraLog(String lote) throws Exception{
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("TABELA", "TGFEST");
			VO.setProperty("CAMPO", "CONTROLE");
			VO.setProperty("VLROLD", "");
			VO.setProperty("VLRNEW", "");
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("PKTABELA", lote);
			VO.setProperty("OBSERVACAO", "BOTAO AJUSTA LOTE - LOTE:"+lote);
			
			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		

	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private void excluiStatusLote(String controle) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("Lote", "this.CONTROLE=?",new Object[] {controle}));
			
		} catch (Exception e) {
			System.out.println("## [br.tcibem.btn_ajustaLote] ## - Nao foi possivel excluir o status do lote!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void cadastraStatusLote(String controle, String status, String empresa) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Lote");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODPROD", getTCIBEM(controle).asBigDecimal("CODPROD"));
			VO.setProperty("CODEMP", new BigDecimal(empresa));
			VO.setProperty("CODLOCAL", new BigDecimal(1500));
			VO.setProperty("CONTROLE", controle);
			VO.setProperty("TITULO", "Status");
			VO.setProperty("LOGICO", "S");
			VO.setProperty("TEXTO", status);
			
			dwfFacade.createEntity("Lote", (EntityVO) VO);
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
