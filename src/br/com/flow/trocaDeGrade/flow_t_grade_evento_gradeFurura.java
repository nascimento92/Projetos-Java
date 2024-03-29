package br.com.flow.trocaDeGrade;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
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
		deleteX(arg0);
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		insertX(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		updateX(arg0);
	}

	public void insertX(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idflow = VO.asBigDecimal("IDINSTPRN");
		String tecla = VO.asString("TECLA");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		
		BigDecimal valorFuncionario = BigDecimal.ZERO;
		BigDecimal valorParceiro = BigDecimal.ZERO;
		
		if(VO.asBigDecimal("VLRFUN")==null) {
			valorFuncionario = getValorFuncionario(idflow,tecla,produto);
		}else {
			valorFuncionario = VO.asBigDecimal("VLRFUN");
		}
		
		if(VO.asBigDecimal("VLRPARC")!=null) {
			valorParceiro = VO.asBigDecimal("VLRPARC");
		}
		
		VO.setProperty("VLRFUN", valorFuncionario);
		VO.setProperty("VLRPARC", valorParceiro);	
		VO.setProperty("MOLANOVA", "S");
		
		BigDecimal nivelpar = VO.asBigDecimal("NIVELPAR");
		BigDecimal capacidade = VO.asBigDecimal("CAPACIDADE");
		BigDecimal nivelalerta = VO.asBigDecimal("NIVELALERTA");
		
		if(nivelpar.intValue()>capacidade.intValue()) {
			throw new Error("<b>Nivel par n�o pode ser maior que a capacidade!");
		}
		
		if(nivelalerta.intValue()>nivelpar.intValue()) {
			throw new Error("<b>Nivel alerta n�o pode ser maior que o Nivel par!");
		}
		

		if (!"0".equals(tecla)) { // impede que sejam cadastradas teclas iguais para as m�quinas (GCE n�o entra na valida��o)
			
			if (validaSeJaExisteAhTecla(idflow, tecla)) {
				throw new Error("A tecla <b>" + tecla + "</b> j� est� cadastrada para o patrim�nio!");
			}

			if (validaSeJaExisteOhProduto(idflow, produto, tecla)) {
				VO.setProperty("PRODREPETIDO", "S");
			}
			
			if(validaSeEhUmaMolaDupla(produto)) {
				VO.setProperty("MOLADUPLA", "S");
			}

			salvaDadosAlterados(VO, idflow, produto, tecla, "Nova tecla/Mola", valorFuncionario,valorParceiro);
		}

		if ("0".equals(tecla)) {
			if (validaSeJaExisteOhProduto(idflow, produto)) {
				throw new Error("O Produto <b>" + produto + "</b> j� est� cadastrada para o patrim�nio!");
			}
			
			if(validaSeEhUmaMolaDupla(produto)) {
				VO.setProperty("MOLADUPLA", "S");
			}

			salvaDadosAlterados(VO, idflow, produto, tecla, "Novo Produto", valorFuncionario,valorParceiro);
		}

	}
	
	public void updateX(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idflow = VO.asBigDecimal("IDINSTPRN");
		String patrimonio = getPatrimonio(idflow);
	
		BigDecimal nivelpar = VO.asBigDecimal("NIVELPAR");
		BigDecimal capacidade = VO.asBigDecimal("CAPACIDADE");
		BigDecimal nivelalerta = VO.asBigDecimal("NIVELALERTA");
		
		if(nivelpar.intValue()>capacidade.intValue()) {
			throw new Error("<b>Nivel par n�o pode ser maior que a capacidade!");
		}
		
		if(nivelalerta.intValue()>nivelpar.intValue()) {
			throw new Error("<b>Nivel alerta n�o pode ser maior que o Nivel par!");
		}
		
		if(verificaSeEstaNaTelaInstalacoes(patrimonio)) {
			if("S".equals(verificaSeEhUmTotem(patrimonio))) {
				atualizaTotens(arg0, patrimonio);
			}else {
				atualizaMaquinas(arg0, patrimonio);
			}
		}else {
			atualizaMaquinas(arg0, patrimonio);
		}
	}
	
	public void atualizaTotens(PersistenceEvent arg0, String patrimonio) throws Exception {
		
		//new
		DynamicVO newVO = (DynamicVO) arg0.getVo();	
		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		
		BigDecimal valorFuncionario = BigDecimal.ZERO;
		BigDecimal valorParceiro = BigDecimal.ZERO;
		
		if(newVO.asBigDecimal("VLRFUN")!=null) {
			valorFuncionario = newVO.asBigDecimal("VLRFUN");		
		}else {
			valorFuncionario = getValorFuncionario(idflow,tecla,produto);
			newVO.setProperty("VLRFUN", valorFuncionario);
		}
		
		if(newVO.asBigDecimal("VLRPARC")!=null) {
			valorParceiro = newVO.asBigDecimal("VLRPARC");
		}
		
		if(!tecla.equals("0")) {
			throw new Error("O patrimonio "+patrimonio+" � um totem, a tecla n�o pode ser diferente de 0 (zero)! "+tecla);
		}
		
		//old
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		BigDecimal produtoAnterior = oldVO.asBigDecimal("CODPROD");
		String teclaAnterior = oldVO.asString("TECLA");
		BigDecimal valorFunAnterior = oldVO.asBigDecimal("VLRFUN");
		BigDecimal valorParAnterior = oldVO.asBigDecimal("VLRPARC");
		
		if(validaSeOhProdutoExistiaTotem(idflow,produtoAnterior)) { //produto anterior
			
			if(produto!=produtoAnterior) {
				
				if(validaSeEhUmaMolaDupla(produto)) {
					newVO.setProperty("MOLADUPLA", "S");
				}
				
				deletaDadoAnterior(idflow,teclaAnterior,produtoAnterior);
				salvaDadosAlterados(oldVO, idflow, produtoAnterior, teclaAnterior, "Retirar Produto", valorFunAnterior,valorParAnterior);
			}
			
		}
		
		if(!validaSeOhProdutoExistiaTotem(idflow, produto)) { //produto novo
			deletaDadoAnterior(idflow,tecla,produto);
			salvaDadosAlterados(newVO, idflow, produto, tecla, "Novo Produto", valorFuncionario,valorParceiro);
		}else { //j� existia
			DynamicVO TeclaOriginal = getTeclaAnterior(idflow, tecla, produto);
			
			realizaAlteracao(newVO,TeclaOriginal);
		}
	}
	
	public void atualizaMaquinas(PersistenceEvent arg0, String patrimonio) throws Exception, Error {
		
		//new
		DynamicVO newVO = (DynamicVO) arg0.getVo();	
		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		
		//old
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		BigDecimal produtoAnterior = oldVO.asBigDecimal("CODPROD");
		String teclaAnterior = oldVO.asString("TECLA");
		
		
		if (validaSeJaExisteOhProduto(idflow, produto, tecla)) {
			newVO.setProperty("PRODREPETIDO", "S");
		}
		
		if(validaSeEhUmaMolaDupla(produto)) {
			newVO.setProperty("MOLADUPLA", "S");
		}
		
		
		if (!"0".equals(tecla)) { // impede que sejam cadastradas teclas iguais para as m�quinas (GCE n�o entra na
									// valida��o)
			if (tecla != teclaAnterior) {

				if (validaSeJaExisteAhTecla(idflow, tecla)) {
					throw new Error("A tecla <b>" + tecla + "</b> j� est� cadastrada para o patrim�nio!");
				}
				
				//deletaDadoAnterior(idflow,teclaAnterior,produto);
			}

			
		} else {
			throw new Error(
					"O patrim�nio " + patrimonio + " n�o � um totem, a sua tecla <b>n�o</b> pode ser 0 (zero)!");
		}
		
		if(validaSeJaExistiaAhTecla(idflow, tecla)) { //mesma tecla
			
			if(validaSeJaExistiaAhTeclaEOhProduto(idflow, tecla, produto)) { //mesma tecla, mesmo produto
				DynamicVO TeclaOriginal = getTeclaAnterior(idflow, tecla, produto);
				realizaAlteracao(newVO,TeclaOriginal);
			}else { // mesma tecla, produto diferente
				DynamicVO TeclaOriginal = getTeclaAnterior(idflow, tecla);
				BigDecimal produtoOriginal = TeclaOriginal.asBigDecimal("CODPROD");
				
				deletaDadoAnterior(idflow,teclaAnterior,produtoAnterior);
				deletaDadoAnterior(idflow,tecla,produtoOriginal);
				salvaDadosAlterados(TeclaOriginal, idflow,produtoOriginal,tecla,"Retirar Produto."); //registrar retirada do prod. anterior
				
				realizaAlteracao(newVO,TeclaOriginal); //registra novo produto.
			}
		
		}else { //tecla diferente
			deletaDadoAnterior(idflow,teclaAnterior,produtoAnterior);
			deletaDadoAnterior(idflow,tecla,produto);
			salvaDadosAlterados(newVO, idflow,produto,tecla,"Nova Tecla / Mola.");
		}
		
		
	}
	
	private void realizaAlteracao(DynamicVO novaTecla, DynamicVO TeclaOriginal) {
		
		BigDecimal idflow = novaTecla.asBigDecimal("IDINSTPRN");
		
		try {
			String retorno = " ";
			
			//nova
			BigDecimal produto = novaTecla.asBigDecimal("CODPROD");
			String tecla = novaTecla.asString("TECLA");
			BigDecimal capacidade = novaTecla.asBigDecimal("CAPACIDADE");
			BigDecimal nivelpar = novaTecla.asBigDecimal("NIVELPAR");
			BigDecimal nivelalerta = novaTecla.asBigDecimal("NIVELALERTA");
			BigDecimal valorFuncionario = BigDecimal.ZERO;
			BigDecimal valorParceiro = BigDecimal.ZERO;
			
			if(novaTecla.asBigDecimal("VLRFUN")!=null) {
				valorFuncionario = novaTecla.asBigDecimal("VLRFUN");		
			}else {
				valorFuncionario = getValorFuncionario(idflow,tecla,produto);
				novaTecla.setProperty("VLRFUN", valorFuncionario);
			}
			
			if(novaTecla.asBigDecimal("VLRPARC")!=null) {
				valorParceiro = novaTecla.asBigDecimal("VLRPARC");
			}
			
			//anterior
			BigDecimal produtoOr = TeclaOriginal.asBigDecimal("CODPROD");
			BigDecimal capacidadeOr = TeclaOriginal.asBigDecimal("CAPACIDADE");
			BigDecimal nivelparOr = TeclaOriginal.asBigDecimal("NIVELPAR");
			BigDecimal nivelalertaOr = TeclaOriginal.asBigDecimal("NIVELALERTA");
			BigDecimal vlrfunOr = TeclaOriginal.asBigDecimal("VLRFUN");
			BigDecimal vlrparOr = TeclaOriginal.asBigDecimal("VLRPARC");
			
			/**
			 * se algum desses campos for alterado, vai prejudicar o flow da troca de grade, uma vez que ele faz uma consulta baseado nas descri��es.
			 */
			
			if(produto.intValue()!=produtoOr.intValue()) {
				retorno+=" Novo Produto,";
			}
			
			if(capacidade.intValue()>capacidadeOr.intValue()) {
				retorno+=" <b>Aumento</b> Capacidade/Mola,";
			}
			
			if(capacidade.intValue()<capacidadeOr.intValue()) {
				retorno+=" <u>Redu��o</u> Capacidade/Mola,";
			}
			
			if(nivelpar.intValue()>nivelparOr.intValue()) {
				retorno+=" <b>Aumento</b> NivelPar,";
			}
			
			if(nivelpar.intValue()<nivelparOr.intValue()) {
				retorno+=" <u>Redu��o</u> NivelPar,";
			}
			
			if(nivelalerta.intValue()>nivelalertaOr.intValue()) {
				retorno+=" <b>Aumento</b> NivelAlerta,";
			}
			
			if(nivelalerta.intValue()<nivelalertaOr.intValue()) {
				retorno+=" <u>Redu��o</u> NivelAlerta,";
			}
			
			if(valorFuncionario.doubleValue()>vlrfunOr.doubleValue()) {
				retorno+=" <b>Aumento</b> Valor Funcionario,";
			}
			
			if(valorFuncionario.doubleValue()<vlrfunOr.doubleValue()) {
				retorno+=" <u>Redu��o</u> Valor Funcionario,";
			}
			
			if(valorParceiro.doubleValue()>vlrparOr.doubleValue()) {
				retorno+=" <b>Aumento</b> Valor Parceiro,";
			}
			
			if(valorParceiro.doubleValue()<vlrparOr.doubleValue()) {
				retorno+=" <u>Redu��o</u> Valor Parceiro,";
			}
			
			deletaDadoAnterior(idflow,tecla,produto);
			
			if(!retorno.equals(" ")) {		
				salvaDadosAlterados(novaTecla, idflow, produto, tecla, retorno, valorFuncionario,valorParceiro);
			}	
			
		} catch (Exception e) {
			salvarException("[realizaAlteracao] Nao foi possivel realizar a altera��o, flow: "+idflow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private boolean validaSeOhProdutoExistiaTotem(BigDecimal idflow, BigDecimal produto) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND CODPROD=?", new Object[] { idflow,produto });
		if (VO != null) {
			valida = true;
		}

		return valida;
		
	}
	
	private String verificaSeEhUmTotem(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		String totem = VO.asString("TOTEM");
		if(totem==null) {
			totem = "N";
		}
		
		return totem;
	}
	
	private boolean verificaSeEstaNaTelaInstalacoes(String patrimonio) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		if(VO!=null) {
			valida = true;
		}
		
		return valida;
	}
	
	private String getPatrimonio(BigDecimal idFlow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_MAQUINASTGRADE");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idFlow });
		String patrimonio = VO.asString("CODBEM");
		
		return patrimonio;
	}
		
	public void deletaDadoAnterior(BigDecimal idflow, String tecla, BigDecimal codprod) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PRODUTOSALTERADOS",
					"this.IDINSTPRN=? AND this.TECLA=? AND this.CODPROD=?", new Object[] { idflow, tecla, codprod }));
		} catch (Exception e) {
			salvarException("[delteraDadoAnterior] Nao foi possivel deletar a tecla: " + tecla + " flow: " + idflow
					+ "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	public boolean validaSeJaExisteAhTecla(BigDecimal idflow, String tecla) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("AD_GRADEFUTURA");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=?", new Object[] { idflow, tecla });

		if (VO != null) {
			valida = true;
		}

		return valida;
	}
	
	public boolean validaSeJaExistiaAhTeclaEOhProduto(BigDecimal idflow, String tecla, BigDecimal produto) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=? AND CODPROD=?", new Object[] { idflow, tecla, produto });

		if (VO != null) {
			valida = true;
		}

		return valida;
	}
	
	public boolean validaSeJaExistiaAhTecla(BigDecimal idflow, String tecla) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=?", new Object[] { idflow, tecla });

		if (VO != null) {
			valida = true;
		}

		return valida;
	}

	public boolean validaSeJaExisteOhProduto(BigDecimal idflow, BigDecimal produto) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("AD_GRADEFUTURA");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND CODPROD=?", new Object[] { idflow, produto });

		if (VO != null) {
			valida = true;
		}

		return valida;
	}
	
	public boolean validaSeJaExisteOhProduto(BigDecimal idflow, BigDecimal produto, String tecla) throws Exception {
		boolean valida = false;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) FROM AD_GRADEFUTURA WHERE IDINSTPRN="+idflow+" AND CODPROD="+produto+" AND TECLA NOT IN ('"+tecla+"')");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			int count = contagem.getInt("COUNT(*)");
			if (count >= 1) {
				valida = true;
			}
		}

		return valida;
	}
	
	public boolean validaSeEhUmaMolaDupla(BigDecimal produto) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });

		if (VO != null) {
			String mola = VO.asString("AD_MOLADUPLA");
			if("S".equals(mola)) {
				valida = true;
			}
		}

		return valida;
	}

	public void salvaDadosAlterados(DynamicVO newVO, BigDecimal idflow, BigDecimal codprod, String tecla, String tipo, BigDecimal valorFuncionario, BigDecimal valorParceiro) {
		
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
			VO.setProperty("VLRFUN", valorFuncionario);
			VO.setProperty("NIVELPAR", newVO.asBigDecimal("NIVELPAR"));
			VO.setProperty("CAPACIDADE", newVO.asBigDecimal("CAPACIDADE"));
			VO.setProperty("NIVELALERTA", newVO.asBigDecimal("NIVELALERTA"));
			VO.setProperty("VLRPARC", valorParceiro);
			VO.setProperty("TIPO", tipo);

			if ("S".equals(newVO.asString("PRODREPETIDO"))) {
				VO.setProperty("PRODREPETIDO", "S");
			}

			dwfFacade.createEntity("AD_PRODUTOSALTERADOS", (EntityVO) VO);

		} catch (Exception e) {
			salvarException(""
					+ "[salvaDadosAlterados] Nao foi possivel salvar a tecla: " + tecla + 
					" flow: " + idflow +
					" IDINSTTAR: "+newVO.asBigDecimal("IDINSTTAR")+
					" CODPROD: "+codprod+
					" TECLA: "+tecla+
					" CODREGISTRO: "+newVO.asBigDecimal("CODREGISTRO")+
					" IDTAREFA: "+newVO.asString("IDTAREFA")+
					" VLRFUN: "+valorFuncionario+
					" NIVELPAR: "+newVO.asBigDecimal("NIVELPAR")+
					" CAPACIDADE: "+newVO.asBigDecimal("CAPACIDADE")+
					" NIVELALERTA: "+newVO.asBigDecimal("NIVELALERTA")+
					" VLRPARC: "+valorParceiro+
					" TIPO: "+tipo+
					" PRODREPETIDO: "+newVO.asString("PRODREPETIDO")+
					"\n" + e.getMessage() + "\n" + e.getCause());
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
			salvarException(""
					+ "[salvaDadosAlterados] Nao foi possivel salvar a tecla: " + tecla + 
					" flow: " + idflow +
					" IDINSTTAR: "+newVO.asBigDecimal("IDINSTTAR")+
					" CODPROD: "+codprod+
					" TECLA: "+tecla+
					" CODREGISTRO: "+newVO.asBigDecimal("CODREGISTRO")+
					" IDTAREFA: "+newVO.asString("IDTAREFA")+
					" VLRFUN: "+newVO.asBigDecimal("VLRFUN")+
					" NIVELPAR: "+newVO.asBigDecimal("NIVELPAR")+
					" CAPACIDADE: "+newVO.asBigDecimal("CAPACIDADE")+
					" NIVELALERTA: "+newVO.asBigDecimal("NIVELALERTA")+
					" VLRPARC: "+newVO.asBigDecimal("VLRPARC")+
					" TIPO: "+tipo+
					" PRODREPETIDO: "+newVO.asString("PRODREPETIDO")+
					"\n" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private BigDecimal getValorFuncionario(BigDecimal idFlow, String tecla, BigDecimal produto) {
		BigDecimal valor = BigDecimal.ZERO;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT "+
			"NVL(NVL("+
			"(SELECT VLRFUN FROM AD_TECLAS WHERE CODBEM=M.CODBEM AND TECLA="+tecla+" AND CODPROD="+produto+"),"+
			"(SELECT VALOR FROM GC_TP_VALOR_PRODUTOS WHERE CODBEM=M.CODBEM AND CODPROD="+produto+")"+
			"),0) AS VLRFUN "+
			"FROM AD_MAQUINASTGRADE M "+
			"WHERE M.IDINSTPRN="+idFlow);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				valor = contagem.getBigDecimal("VLRFUN");

			}
		} catch (Exception e) {
			salvarException("[getValorFuncionario] Nao foi possivel obter o valor da tecla: " + tecla + " flow: " + idFlow + " produto: "+produto
					+ "\n" + e.getMessage() + "\n" + e.getCause());
		}
		
		return valor;
	}
	
	private DynamicVO getTeclaAnterior(BigDecimal idflow, String tecla, BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=? AND CODPROD=?", new Object[] { idflow, tecla, produto });
		
		return VO;
	}
	
	private DynamicVO getTeclaAnterior(BigDecimal idflow, String tecla) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_GRADEATUAL");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=?", new Object[] { idflow, tecla});
		
		return VO;
	}

	public void atualizaValoresAlterados(BigDecimal idflow, String tecla, BigDecimal produto, DynamicVO newVO) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PRODUTOSALTERADOS",
					"this.IDINSTPRN=? AND this.TECLA=? AND this.CODPROD=? ", new Object[] { idflow, tecla, produto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("TECLA", newVO.asString("TECLA"));
				VO.setProperty("CODPROD", newVO.asBigDecimal("CODPROD"));
				VO.setProperty("CAPACIDADE", newVO.asBigDecimal("CAPACIDADE"));
				VO.setProperty("NIVELPAR", newVO.asBigDecimal("NIVELPAR"));
				VO.setProperty("NIVELALERTA", newVO.asBigDecimal("NIVELALERTA"));
				VO.setProperty("VLRFUN", newVO.asBigDecimal("VLRFUN"));
				VO.setProperty("VLRPARC", newVO.asBigDecimal("VLRPARC"));
				
				if("S".equals(newVO.asString("PRODREPETIDO"))) {
					VO.setProperty("PRODREPETIDO", "S");
				}

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaDadosAlterados] Nao foi possivel salvar a tecla: " + tecla + " flow: " + idflow+ " produto: "+produto+
					 "\n" + e.getMessage() + "\n" + e.getCause());
	
		}
	}
	
	public void deleteX(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();

		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		
		if(validaSeExiste(idflow,tecla,produto)) {
			deletaDadoAnterior(idflow, tecla, produto);
		}
		
		//TODO :: Registrar produto para ser retirado.
		salvaDadosAlterados(newVO, idflow,produto,tecla,"Retirar Produto.");

	}
	
	public boolean validaSeExiste(BigDecimal idflow, String tecla, BigDecimal produto) {
		boolean valida = false;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("AD_PRODUTOSALTERADOS");
			DynamicVO VO = DAO.findOne("IDINSTPRN=? AND TECLA=? AND CODPROD=?",new Object[] { idflow,tecla,produto });

			if(VO!=null) {
				valida = true;
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
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
			// aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
