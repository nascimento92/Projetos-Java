package br.com.buttons;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnAjustarEnderecoEntrega implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		int qtdLinhas = arg0.getLinhas().length;

		if (qtdLinhas > 550) {
			arg0.mostraErro("Favor selecionar no m·ximo 550 linhas de cada vez");
		}

		Registro[] linhas = arg0.getLinhas();

		atualizaEnderecoEntrega(linhas, arg0);
		atualizaEnderecoPrincipal(linhas, arg0);
		
		arg0.setMensagemRetorno(linhas.length+" Parceiros Atualizados !");

	}

	private void atualizaEnderecoEntrega(Registro[] linhas, ContextoAcao arg0) throws Exception {
		
		BigDecimal codparc = new BigDecimal(0);
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
	
			for (int i = 0; i < linhas.length; i++) {

				Collection<?> parceiro = dwfEntityFacade
						.findByDynamicFinder(new FinderWrapper("Parceiro",
								"this.CODPARC = ? ", new Object[] { linhas[i]
										.getCampo("CODPARC") }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity
							.getValueObject()).wrapInterface(DynamicVO.class);

					codparc = (BigDecimal) DynamicVO.getProperty("CODPARC");

					if (validaSeExisteNaTGFCPL(codparc)) {

						System.out.println("Atualizando Parceiro:" + codparc);

						String cepEntrega = getCepEntrega(codparc);
						
						if(cepEntrega!=null){
							
							BigDecimal codbairro = getCodigo(cepEntrega, "CODBAI");
							BigDecimal codcidade = getCodigo(cepEntrega, "CODCID");
							BigDecimal codendereco = getCodigo(cepEntrega, "CODEND");
							
							if (!validaSeJaExisteOhEndereco(codendereco, "Endereco",
									"CODEND")
									|| !validaSeJaExisteOhEndereco(codcidade, "Cidade",
											"CODCID")
									|| !validaSeJaExisteOhEndereco(codbairro, "Bairro",
											"CODBAI")) {
								apagaCEP(cepEntrega);
							}

							if (!existeCEP(cepEntrega)) {
								incluiCEP(cepEntrega, arg0);
							}

							codbairro = getCodigo(cepEntrega, "CODBAI");
							codcidade = getCodigo(cepEntrega, "CODCID");
							codendereco = getCodigo(cepEntrega, "CODEND");
							
							salvaDadosTgfcpl(codparc,codbairro,codcidade,codendereco);
							
						}	
					}
				}
			}
			
		} catch (Exception e) {
			arg0.mostraErro("N„o foi possivel atualizar o parceiro: "+codparc+" \n\nerro: "+e);
		}
		

	}
	
	private void atualizaEnderecoPrincipal(Registro[] linhas, ContextoAcao arg0)throws Exception {
		
		BigDecimal codparc = new BigDecimal(0);
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			for (int i = 0; i < linhas.length; i++) {

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Parceiro","this.CODPARC = ? ", new Object[] { linhas[i].getCampo("CODPARC") }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
					
					String CEP = (String) DynamicVO.getProperty("CEP");
					codparc = (BigDecimal) DynamicVO.getProperty("CODPARC");
					
					BigDecimal codbairro = getCodigo(CEP, "CODBAI");
			        BigDecimal codcidade = getCodigo(CEP, "CODCID");
			        BigDecimal codendereco = getCodigo(CEP, "CODEND");
			        
			        if (!validaSeJaExisteOhEndereco(codendereco, "Endereco", "CODEND") || 
			  	          !validaSeJaExisteOhEndereco(codcidade, "Cidade", "CODCID") || 
			  	          !validaSeJaExisteOhEndereco(codbairro, "Bairro", "CODBAI"))
			  	        {
			  	          apagaCEP(CEP);
			  	        }
					
			        if (!existeCEP(CEP)) {
				          incluiCEP(CEP, arg0);
				        }
			        
			        codbairro = getCodigo(CEP, "CODBAI");
			        codcidade = getCodigo(CEP, "CODCID");
			        codendereco = getCodigo(CEP, "CODEND");
			        
			        linhas[i].setCampo("CODBAI", codbairro);
			        linhas[i].setCampo("CODCID", codcidade);
			        linhas[i].setCampo("CODEND", codendereco);
			        linhas[i].save();

				}
			}
			
		} catch (Exception e) {
			arg0.mostraErro("N„o foi possivel atualizar o parceiro: "+codparc+" \n\nerro: "+e);
		}
		

	}

	public boolean validaSeExisteNaTGFCPL(BigDecimal codparc) throws Exception {
		boolean existe = false;

		JapeWrapper RecorrenciaDAO = JapeFactory.dao("ComplementoParc");
		DynamicVO TGFCPLVO = RecorrenciaDAO.findOne("CODPARC=?",
				new Object[] { codparc });

		if (TGFCPLVO != null) {
			existe = true;
		} else {
			existe = false;
		}

		return existe;
	}

	private String getCepEntrega(BigDecimal codparc) throws Exception {

		JapeWrapper RecorrenciaDAO = JapeFactory.dao("ComplementoParc");
		DynamicVO TGFCPLVO = RecorrenciaDAO.findOne("CODPARC=?",
				new Object[] { codparc });

		String cepEntrega = TGFCPLVO.asString("CEPENTREGA");

		return cepEntrega;
	}
	
	private void salvaDadosTgfcpl(BigDecimal codparc, BigDecimal codbairro, BigDecimal codcidade, BigDecimal codendereco) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("ComplementoParc", codparc);
		EntityVO NVO = persistentLocalEntity.getValueObject();
		DynamicVO tgfcplVO = (DynamicVO) NVO;
		
		tgfcplVO.setProperty("CODENDENTREGA", codendereco);
		tgfcplVO.setProperty("CODBAIENTREGA", codbairro);
		tgfcplVO.setProperty("CODCIDENTREGA", codcidade);
		
		persistentLocalEntity.setValueObject(NVO);
		
	}

	private BigDecimal getCodigo(String CEP, String Campo) throws Exception {
		BigDecimal codigo = new BigDecimal(0);
		JapeWrapper cepDAO = null;
		cepDAO = JapeFactory.dao("CEP");
		DynamicVO cep = null;
		cep = cepDAO.findOne(" CEP=? and rownum=1", new Object[] { CEP });
		if (cep != null) {
			codigo = cep.asBigDecimal(Campo);
		}

		return codigo;
	}

	private boolean validaSeJaExisteOhEndereco(BigDecimal codigo,
			String Entidade, String Campo) throws Exception {

		boolean ok = false;

		JapeWrapper EntityDAO = null;
		EntityDAO = JapeFactory.dao(Entidade);
		DynamicVO Entity = null;
		Entity = EntityDAO.findOne(String.valueOf(Campo) + " =? ",
				new Object[] { codigo });
		if (Entity != null) {
			ok = true;
			System.out.println(String.valueOf(Entidade) + " j· Existe!");
		}

		return ok;
	}

	private static void apagaCEP(String CEP) throws Exception {
		JdbcWrapper jdbcWrapper = null;

		try {
			jdbcWrapper = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
			jdbcWrapper.openSession();
			StringBuffer buf = new StringBuffer(
					"DELETE FROM TSICEP WHERE CEP = ? ");
			PreparedStatement ps = jdbcWrapper.getPreparedStatement(buf
					.toString());
			ps.setString(1, CEP);
			ps.executeUpdate();
		} finally {

			if (jdbcWrapper != null) {
				jdbcWrapper.closeSession();
			}
		}
	}

	private void incluiCEP(String CEP, ContextoAcao arg0) throws Exception {

		CepWebService cepWebService = new CepWebService(CEP);
		String cidade = cepWebService.getCidade();
		String estado = cepWebService.getEstado();
		String bairro = cepWebService.getBairro();
		String logradouro = cepWebService.getLogradouro();
		String ibge = cepWebService.getIBGE();

		int indice = logradouro.indexOf(" ");

		String tipo = transformaString(logradouro.substring(0, indice));
		String endereco = transformaString(logradouro.substring(indice + 1));

		BigDecimal codcidade = getCodigoCidade("Cidade", "CODCID", cidade,
				estado, arg0);
		BigDecimal codbairro = getCodigo("Bairro", "CODBAI", bairro);
		BigDecimal codendereco = getCodigo("Endereco", "CODEND", endereco);

		if (codcidade.equals(new BigDecimal(0))) {
			incluiCidade(transformaString(cidade), transformaString(estado),
					ibge, arg0);
		}

		if (codbairro.equals(new BigDecimal(0))) {
			incluiBairro(transformaString(bairro), arg0);
		}

		if (codendereco.equals(new BigDecimal(0))) {
			incluiEndereco(transformaString(endereco), transformaString(tipo),
					arg0);
		}

		codcidade = getCodigoCidade("Cidade", "CODCID", cidade, estado, arg0);
		codbairro = getCodigo("Bairro", "CODBAI", bairro);
		codendereco = getCodigo("Endereco", "CODEND", endereco);

		incluiCEP(codcidade, codbairro, codendereco, CEP, arg0);
	}

	private String transformaString(String string) {
		string = Normalizer.normalize(string, Normalizer.Form.NFD);
		return string.replaceAll("[^\\p{ASCII}]", "").toUpperCase();
	}

	private BigDecimal getCodigoCidade(String Instancia, String Campo,String Endereco, String Estado, ContextoAcao arg0) throws Exception {
		
		BigDecimal codigo = new BigDecimal(0);
		
		JapeWrapper endDAO = null;
		endDAO = JapeFactory.dao(Instancia);
		DynamicVO end = null;

		String Cidade = transformaString(Endereco);
		String coduf = getCodUF(Estado).toString();

		end = endDAO
				.findOne(
						"TRIM(TRANSLATE(UPPER(DESCRICAOCORREIO),'¡¬¿√… Õ”‘’‹⁄«','AAAAEEIOOOUUC'))=TRIM(TRANSLATE(UPPER(?),'¡¬¿√… Õ”‘’‹⁄«','AAAAEEIOOOUUC')) and UF=? ",
						new Object[] { Cidade, coduf });
		if (end != null) {
			codigo = end.asBigDecimal(Campo);
		}

		return codigo;
	}

	private BigDecimal getCodUF(String UF) throws Exception {
		BigDecimal codigo = new BigDecimal(0);
		JapeWrapper endDAO = null;
		endDAO = JapeFactory.dao("UnidadeFederativa");
		DynamicVO end = null;
		end = endDAO.findOne("UF=?", new Object[] { UF });
		if (end != null) {
			codigo = (BigDecimal) end.getProperty("CODUF");
		}

		return codigo;
	}

	private BigDecimal getCodigo(String Instancia, String Campo, String Endereco)
			throws Exception {
		BigDecimal codigo = new BigDecimal(0);
		JapeWrapper endDAO = null;
		endDAO = JapeFactory.dao(Instancia);
		DynamicVO end = null;
		String Query = "TRIM(TRANSLATE(UPPER(DESCRICAOCORREIO),'¡¬¿√… Õ”‘’‹⁄«','AAAAEEIOOOUUC'))=TRIM(TRANSLATE(UPPER(?),'¡¬¿√… Õ”‘’‹⁄«','AAAAEEIOOOUUC'))";
		String newQery = new String(Query.getBytes("windows-1252"),
				"windows-1252");
		end = endDAO.findOne(newQery, new Object[] { Endereco });
		if (end != null) {
			codigo = end.asBigDecimal(Campo);
		}

		return codigo;
	}

	private void incluiCidade(String Cidade, String Estado, String ibge,ContextoAcao arg0) throws Exception {
		
		Registro item = arg0.novaLinha("TSICID");
		BigDecimal coduf = getCodUF(Estado);
		item.setCampo("UF", coduf);
		item.setCampo("NOMECID", Cidade);
		item.setCampo("DESCRICAOCORREIO", Cidade);
		item.setCampo("DTALTER", new Date());
		item.setCampo("CODMUNFIS", ibge);
		item.save();
	}

	private void incluiBairro(String Bairro, ContextoAcao arg0)throws Exception {
		Registro item = arg0.novaLinha("TSIBAI");
		item.setCampo("NOMEBAI", Bairro);
		item.setCampo("DESCRICAOCORREIO", Bairro);
		item.setCampo("DTALTER", new Date());
		item.save();
	}

	private void incluiEndereco(String Endereco, String Tipo, ContextoAcao arg0)
			throws Exception {
		Registro item = arg0.novaLinha("TSIEND");
		item.setCampo("NOMEEND", Endereco);
		item.setCampo("DESCRICAOCORREIO", Endereco);
		item.setCampo("TIPO", Tipo);
		item.setCampo("DTALTER", new Date());
		item.save();
	}

	private void incluiCEP(BigDecimal CODCID, BigDecimal CODBAI,
			BigDecimal CODEND, String CEP, ContextoAcao arg0) throws Exception {
		Registro item = arg0.novaLinha("TSICEP");
		item.setCampo("CODCID", CODCID);
		item.setCampo("CODBAI", CODBAI);
		item.setCampo("CODEND", CODEND);
		item.setCampo("CEP", CEP);
		item.save();
	}

	private boolean existeCEP(String CEP) throws Exception {
		boolean ok = false;
		JapeWrapper cepDAO = null;
		cepDAO = JapeFactory.dao("CEP");
		DynamicVO cep = null;
		cep = cepDAO.findOne(" CEP=? ", new Object[] { CEP });
		if (cep != null) {
			ok = true;
			System.out.println("CEP j· Existe!");
		}

		return ok;
	}

}
