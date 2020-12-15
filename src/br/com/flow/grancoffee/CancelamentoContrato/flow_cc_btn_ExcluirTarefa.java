package br.com.flow.grancoffee.CancelamentoContrato;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class flow_cc_btn_ExcluirTarefa implements AcaoRotinaJava {

	int cont = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) throws Exception {
		Integer idflow = (Integer) arg0.getParam("FLOW");
		
		excluirInstanciaTarefa(idflow);
		excluirInstanciaVariavel(idflow);
		excluirInstanciaProcesso(idflow);
		excluirAD_OSCANCELAMENTO(idflow);
		excluirAD_PRODCANCELAMENTO(idflow);
		excluirAD_DEVNFCANCELAMENTO(idflow);
		excluirAD_PATCANCELAMENTO(idflow);
		excluirAD_FORMCANCELAMENTO(idflow);
		excluirAD_EMAILSFLOW(idflow);
		excluirAD_GERENCIAINSTPEDIDO(idflow);
		excluirAD_GERENCIAINSTPAT(idflow);
		excluirAD_GERENCIAINST(idflow);
		excluirAD_MAQUINASFLOW(idflow);

		if (this.cont > 3) {
			arg0.setMensagemRetorno("<br/><b>Flow Excluido!</b><br/>");
		} else {
			arg0.mostraErro("<br/><b>Ops, algo deu errado, procurar o setor de TI!</b><br/>");
		}
	}

	private void excluirInstanciaTarefa(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("InstanciaTarefa", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException("[excluirInstanciaTarefa] Nao foi possivel excluir os dados do flow! " + e.getMessage()
					+ "\n" + e.getCause());
		}
	}

	private void excluirInstanciaVariavel(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("InstanciaVariavel", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirInstanciaVariavel] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirInstanciaProcesso(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("InstanciaProcesso", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirInstanciaProcesso] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirAD_OSCANCELAMENTO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_OSCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;
	
		} catch (Exception e) {
			salvarException(
					"[excluirAD_OSCANCELAMENTO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirAD_PRODCANCELAMENTO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PRODCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_PRODCANCELAMENTO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirAD_DEVNFCANCELAMENTO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_DEVNFCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_DEVNFCANCELAMENTO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirAD_PATCANCELAMENTO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PATCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_PATCANCELAMENTO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void excluirAD_FORMCANCELAMENTO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_FORMCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_FORMCANCELAMENTO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void excluirAD_EMAILSFLOW(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_EMAILSFLOW", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_EMAILSFLOW] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void excluirAD_GERENCIAINSTPEDIDO(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GERENCIAINSTPEDIDO", "this.IDFLOW=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_GERENCIAINSTPEDIDO] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void excluirAD_GERENCIAINSTPAT(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GERENCIAINSTPAT", "this.IDFLOW=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_GERENCIAINSTPAT] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void excluirAD_GERENCIAINST(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GERENCIAINST", "this.IDFLOW=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_GERENCIAINST] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void excluirAD_MAQUINASFLOW(Integer idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_MAQUINASFLOW", "this.IDINSTPRN=?",new Object[] {idflow}));
			cont++;

		} catch (Exception e) {
			salvarException(
					"[excluirAD_MAQUINASFLOW] Nao foi possivel excluir os dados do flow! " + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_cc_btn_ExcluirTarefa");
			VO.setProperty("PACOTE", "br.com.flow.grancoffee.CancelamentoContrato");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
