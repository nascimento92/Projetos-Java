package br.com.flow.trocaDeGrade;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class flow_t_grade_evento_gradeFurura implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		delete(arg0);
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}
	
	public void delete(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		
		delteraDadoAnterior(idflow,tecla,produto);
	}
	
	public void insert(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		String tipo = "Produto Novo";
		
		salvaDadosAlterados(newVO,idflow,produto,tecla,tipo);
	}
	
	public void update(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();

		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");

		String tecla = newVO.asString("TECLA");
		if (tecla != "0") {
			validarAlteracao(idflow, tecla, produto, newVO, oldVO);
		}
	}

	public void validarAlteracao(BigDecimal idflow, String tecla, BigDecimal produto, DynamicVO newVO,
			DynamicVO oldVO) {
		try {

			JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
			DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=?", new Object[] { idflow, tecla });

			BigDecimal produtoAnterior = VO.asBigDecimal("CODPROD");

			if (produto.intValue() != produtoAnterior.intValue()) {
				delteraDadoAnterior(idflow, tecla, oldVO.asBigDecimal("CODPROD"));
				salvaDadosAlterados(newVO, idflow, produto, tecla, "Produto Novo");
				salvaDadosAlterados(newVO, idflow, produtoAnterior, tecla, "Produto p/ Retirar");
			}

			if (produto.intValue() == produtoAnterior.intValue()) {
				delteraDadoAnterior(idflow, tecla, produto);
				delteraDadoAnterior(idflow, tecla, oldVO.asBigDecimal("CODPROD"));

				BigDecimal nivelpar = newVO.asBigDecimal("NIVELPAR");
				BigDecimal nivelparAnterior = VO.asBigDecimal("NIVELPAR");

				BigDecimal valor = newVO.asBigDecimal("VLRFUN").add(newVO.asBigDecimal("VLRPARC"));
				BigDecimal valorAnterior = VO.asBigDecimal("VLRFUN").add(VO.asBigDecimal("VLRPARC"));
				
				BigDecimal capacidade = newVO.asBigDecimal("CAPACIDADE");
				BigDecimal capacidadeAnterior = VO.asBigDecimal("CAPACIDADE");

				String retorno = "";

				if (nivelpar.intValue() > nivelparAnterior.intValue()) {
					retorno = retorno + " Aumento NivelPar,";
				} else if (nivelpar.intValue() < nivelparAnterior.intValue()) {
					retorno = retorno + " Redução NivelPar,";
				}

				if (valor.intValue() > valorAnterior.intValue()) {
					retorno = retorno + " Aumento Valor,";
				} else if (valor.intValue() < valorAnterior.intValue()) {
					retorno = retorno + " Redução Valor,";
				}
				
				if(capacidade.intValue() > capacidadeAnterior.intValue()) {
					retorno = retorno + " Aumento Capacidade/Mola,";
				}else if (capacidade.intValue() < capacidadeAnterior.intValue()) {
					retorno = retorno + " Redução Capacidade/Mola,";
				}

				salvaDadosAlterados(newVO, idflow, produto, tecla, retorno);
			}

		} catch (Exception e) {
			salvarException("[validarAlteracao] Nao foi possivel alterar a tecla: " + tecla + " flow: " + idflow + "\n"
					+ e.getMessage() + "\n" + e.getCause());
		}

	}

	public void delteraDadoAnterior(BigDecimal idflow, String tecla, BigDecimal codprod) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PRODUTOSALTERADOS",
					"this.IDINSTPRN=? AND this.TECLA=? AND this.CODPROD=?", new Object[] { idflow, tecla, codprod }));
		} catch (Exception e) {
			salvarException("[delteraDadoAnterior] Nao foi possivel deletar a tecla: " + tecla + " flow: " + idflow
					+ "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	public void salvaDadosAlterados(DynamicVO newVO, BigDecimal idflow, BigDecimal codprod, String tecla, String tipo) {

		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_PRODUTOSALTERADOS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("IDINSTPRN", idflow);
			VO.setProperty("IDINSTTAR", newVO.asBigDecimal("IDINSTTAR"));
			VO.setProperty("CODPROD", codprod);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODREGISTRO", newVO.asBigDecimal("CODREGISTRO"));
			VO.setProperty("IDTAREFA", newVO.asString("IDTAREFA"));
			VO.setProperty("VLRFUN", newVO.asBigDecimal("VLRFUN"));
			VO.setProperty("NIVELPAR", newVO.asBigDecimal("NIVELPAR"));
			VO.setProperty("CAPACIDADE", newVO.asBigDecimal("CAPACIDADE"));
			VO.setProperty("NIVELALERTA", newVO.asBigDecimal("NIVELALERTA"));
			VO.setProperty("VLRPARC", newVO.asBigDecimal("VLRPARC"));
			VO.setProperty("TIPO", tipo);

			dwfFacade.createEntity("AD_PRODUTOSALTERADOS", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[salvaDadosAlterados] Nao foi possivel salvar a tecla: " + tecla + " flow: " + idflow
					+ "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_t_grade_evento_gradeFurura");
			VO.setProperty("PACOTE", "br.com.flow.trocaDeGrade");
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
