package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_teste2 implements AcaoRotinaJava{
	
	int qtdTeclas = 99;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		getListaPendente();		
	}

	private void getListaPendente() {
		String patrimonio="";
	
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODBEM FROM AD_GCGERAROTA");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				patrimonio = contagem.getString("CODBEM");
				buildSolicitacao(patrimonio);
			}
		} catch (Exception e) {
			salvarException("[getListaPendente] Nao foi possivel obter a lista: "+patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
	
	}
	
	// FUN��ES
	
		private void buildSolicitacao(String patrimonio) {
			String valor="";
			String abastecimento="";
			BigDecimal idflow = null;
			String hora = "";
			int QtdDias = 0;
			Timestamp dataAgendamento = null;
			Timestamp dataAtendimento = null;
			int rota = getRota(patrimonio);
			BigDecimal idRetorno = null;
			
				
				try {
					
					EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

					Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_GCGERAROTA","this.CODBEM = ? ", new Object[] { patrimonio }));

					for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

					valor = (String) DynamicVO.getProperty("VALOR");
					abastecimento = (String) DynamicVO.getProperty("ABASTECIMENTO");
					idflow = (BigDecimal) DynamicVO.getProperty("IDFLOW");
					
					hora = valor.substring(1)+":00:00";
					QtdDias = Integer.valueOf(valor.substring(0, 1));
					dataAgendamento = buildData(hora);
					dataAtendimento = TimeUtils.dataAddDay(dataAgendamento, QtdDias);
					
					//String erro = validacoes(patrimonio, abastecimento, dataAgendamento, dataAtendimento, new BigDecimal(rota));
					
					//if(erro=="") {
						
						if("1".equals(abastecimento)) { //Apenas Secos
							idRetorno = cadastrarNovoAbastecimento(patrimonio, "S", "N", idflow);
							if(idRetorno!=null) {
								apenasSecos(idRetorno, patrimonio);
								DynamicVO gcsolicitabast = agendarAbastecimento(patrimonio, TimeUtils.getNow(),dataAgendamento, idRetorno, "S", "N", dataAtendimento);
								registrarAgendamento(patrimonio, dataAgendamento, dataAtendimento, abastecimento, idRetorno, gcsolicitabast.asBigDecimal("ID"), "OK", new BigDecimal(rota));
							}
						}
						
						if ("2".equals(abastecimento)) { // Apenas Congelados
							idRetorno = cadastrarNovoAbastecimento(patrimonio, "N", "S", idflow);
							if(idRetorno!=null) {
								apenasCongelados(idRetorno, patrimonio);
								DynamicVO gcsolicitabast = agendarAbastecimento(patrimonio, TimeUtils.getNow(),dataAgendamento, idRetorno, "N", "S", dataAtendimento);
								registrarAgendamento(patrimonio, dataAgendamento, dataAtendimento, abastecimento, idRetorno, gcsolicitabast.asBigDecimal("ID"), "OK", new BigDecimal(rota));
							}
						}
						
						if ("3".equals(abastecimento)) { // Secos e Congelados
							
							//SECOS
							BigDecimal idSeco = cadastrarNovoAbastecimento(patrimonio, "S", "N", idflow);
							if(idSeco!=null) {
								apenasSecos(idSeco, patrimonio);
								DynamicVO gcsolicitabast = agendarAbastecimento(patrimonio, TimeUtils.getNow(),dataAgendamento, idSeco, "S", "N", dataAtendimento);
								registrarAgendamento(patrimonio, dataAgendamento, dataAtendimento, abastecimento, idSeco, gcsolicitabast.asBigDecimal("ID"), "OK", new BigDecimal(rota));
							}
							
							//CONGELADO
							BigDecimal idCongelado = cadastrarNovoAbastecimento(patrimonio, "N", "S", idflow);
							if(idCongelado!=null) {
								apenasCongelados(idCongelado, patrimonio);
								DynamicVO gcsolicitabast = agendarAbastecimento(patrimonio, TimeUtils.getNow(),dataAgendamento, idCongelado, "N", "S", dataAtendimento);
								registrarAgendamento(patrimonio, dataAgendamento, dataAtendimento, abastecimento, idCongelado, gcsolicitabast.asBigDecimal("ID"), "OK", new BigDecimal(rota));
							}

						}
						
						atualizarInstalacao(patrimonio);
				//	}
					
					}

					
				} catch (Exception e) {
					salvarException("[buildSolicitacao] Nao foi possivel criar a solicita��o: "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
				}	
				
		}
		
		private BigDecimal cadastrarNovoAbastecimento(String patrimonio, String secos, String congelados,BigDecimal idflow) {
			
			BigDecimal idAbastecimento = null;

			try {

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_RETABAST");
				DynamicVO VO = (DynamicVO) NPVO;

				int rota = getRota(patrimonio);

				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("DTSOLICITACAO", TimeUtils.getNow());
				VO.setProperty("STATUS", "1");
				VO.setProperty("SOLICITANTE", new BigDecimal(3082));
				VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
				VO.setProperty("CODPARC", getParceiro(patrimonio));

				if (rota != 0) {
					VO.setProperty("ROTA", new BigDecimal(rota));
				}

				if (idflow != null) {
					VO.setProperty("IDFLOW", idflow);
				}

				VO.setProperty("SECOS", secos);
				VO.setProperty("CONGELADOS", congelados);

				dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

				idAbastecimento = VO.asBigDecimal("ID");

			} catch (Exception e) {
				
				  salvarException("[cadastrarNovoAbastecimento] Nao foi possivel cadastrar um novo abastecimento! "
				  + e.getMessage() + "\n" + e.getCause());
				 
			}

			return idAbastecimento;
		}
		
		private DynamicVO agendarAbastecimento(String patrimonio, Timestamp dtSolicitacao, Timestamp dtAgendamento,
				BigDecimal idAbastecimento, String seco, String congelado, Timestamp dtvisita) {
			
			DynamicVO gc_solicitabast = null;
			
			try {

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
				DynamicVO VO = (DynamicVO) NPVO;

				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("CODUSU", new BigDecimal(3082));
				VO.setProperty("STATUS", "1");
				VO.setProperty("DTSOLICIT", dtSolicitacao);
				VO.setProperty("DTAGENDAMENTO", dtAgendamento);
				VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
				VO.setProperty("IDABASTECIMENTO", idAbastecimento);
				VO.setProperty("REABASTECIMENTO", "S");
				VO.setProperty("APENASVISITA", "N");
				VO.setProperty("AD_NUMCONTRATO", getContrato(patrimonio));
				VO.setProperty("AD_CODPARC", getParceiro(patrimonio));
				
				if(dtvisita!=null) {
					VO.setProperty("AD_DTATENDIMENTO", dtvisita);
				}

				if ("S".equals(seco)) {
					VO.setProperty("AD_TIPOPRODUTOS", "1");
				}

				if ("S".equals(congelado)) {
					VO.setProperty("AD_TIPOPRODUTOS", "2");
				}

				String body = montarBody(patrimonio);
				if (body != null) {
					VO.setProperty("AD_BODYPLANOGRAMA", body.toCharArray());
				}

				dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);

				gc_solicitabast = VO;

			} catch (Exception e) {
				
				  salvarException("[agendarAbastecimento] Nao foi possivel agendar o Abastecimento! patrimonio: "
				  + patrimonio + "\n" + e.getMessage() + "\n" + e.getCause());
				 
			}

			return gc_solicitabast;
		}
		
		private static Timestamp buildData(String hora) {
			String formato1 = "yyyy-MM-dd";
			DateFormat df = new SimpleDateFormat(formato1);
			String dtAtual = df.format(TimeUtils.getNow());
			
			String dataEHora = dtAtual+" "+hora;
			
			Timestamp time = Timestamp.valueOf(dataEHora);
			
			return time;
		}
		
		private void apenasSecos(BigDecimal idAbastecimento, String patrimonio) throws Exception {
			if (idAbastecimento != null) {
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
						new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
							.wrapInterface(DynamicVO.class);

					String tecla = DynamicVO.asString("TECLA");
					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
					BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
					BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
					BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
					BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
					BigDecimal valorFinal = vlrpar.add(vlrfun);
					
						try {

							EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
							EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
							DynamicVO VO = (DynamicVO) NPVO;

							VO.setProperty("ID", idAbastecimento);
							VO.setProperty("CODBEM", patrimonio);
							VO.setProperty("TECLA", tecla);
							VO.setProperty("CODPROD", produto);
							VO.setProperty("CAPACIDADE", capacidade);
							VO.setProperty("NIVELPAR", nivelpar);
							VO.setProperty("VALOR", valorFinal);

							dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

						} catch (Exception e) {
							
							  salvarException(
							  "[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
							  + e.getMessage() + "\n" + e.getCause());
							 
						}
				}
			}
		}
		
		public String montarBody(Object codbem) {

			int cont = 1;
			String head = "{\"planogram\":{\"items_attributes\": [";
			String bottom = "]}}";

			String body = "";

			try {

				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade
						.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
							.wrapInterface(DynamicVO.class);

					String teclaAlternativa = DynamicVO.asString("TECLAALT");
					String tecla = DynamicVO.asBigDecimal("TECLA").toString();
					String name = "";

					if ("0".equals(tecla)) {
						tecla = String.valueOf(cont);
						cont++;
					}

					if (teclaAlternativa != null) {
						name = teclaAlternativa;
					} else {
						name = tecla;
					}

					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");

					if (Iterator.hasNext()) {
						body = body + "{" + "\"type\": \"Coil\"," + "\"name\": \"" + name + "\"," + "\"good_id\": "
								+ getGoodId(produto) + "," + "\"capacity\": "
								+ DynamicVO.asBigDecimal("AD_CAPACIDADE").toString() + "," + "\"par_level\": "
								+ DynamicVO.asBigDecimal("AD_NIVELPAR").toString() + "," + "\"alert_level\": "
								+ DynamicVO.asBigDecimal("AD_NIVELALERTA").toString() + "," + "\"desired_price\": "
								+ DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString() + ","
								+ "\"logical_locator\": " + tecla + "," + "\"status\": \"active\"" + "},";
					} else {
						body = body + "{" + "\"type\": \"Coil\"," + "\"name\": \""
								+ DynamicVO.asBigDecimal("TECLA").toString() + "\"," + "\"good_id\": " + getGoodId(produto)
								+ "," + "\"capacity\": " + DynamicVO.asBigDecimal("AD_CAPACIDADE").toString() + ","
								+ "\"par_level\": " + DynamicVO.asBigDecimal("AD_NIVELPAR").toString() + ","
								+ "\"alert_level\": " + DynamicVO.asBigDecimal("AD_NIVELALERTA").toString() + ","
								+ "\"desired_price\": "
								+ DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString() + ","
								+ "\"logical_locator\": " + tecla + "," + "\"status\": \"active\"" + "}";
					}

				}

			} catch (Exception e) {
				
				  salvarException("[montarBody] nao foi possivel montar o Body! patrimonio:" +
				  codbem + "\n" + e.getMessage() + "\n" + e.getCause());
				 
			}

			return head + body + bottom;

		}
		
		public BigDecimal getGoodId(BigDecimal produto) throws Exception {
			BigDecimal id = null;
			JapeWrapper DAO = JapeFactory.dao("Produto");
			DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });
			BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");

			if (idVerti == null) {
				id = new BigDecimal(179707);
			} else {
				id = idVerti;
			}

			return id;
		}
		
		private void registrarAgendamento(String patrimonio, Timestamp dtagendamento, Timestamp dtatendimento, String abastecimento, BigDecimal idRetorno, BigDecimal idSolicitacao, String obs, BigDecimal rota) {
			try {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_VISITASAGENDADAS");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("DTSOLICIT", TimeUtils.getNow());
				VO.setProperty("DTAGENDAMENTO", dtagendamento);
				VO.setProperty("DTATENDIMENTO", dtatendimento);
				VO.setProperty("TIPO", abastecimento);
				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("IDRETORNO", idRetorno);
				VO.setProperty("IDSOLICITACAO", idSolicitacao);
				VO.setProperty("OBS", obs);
				VO.setProperty("ROTA", rota);
				
				dwfFacade.createEntity("AD_VISITASAGENDADAS", (EntityVO) VO);
			} catch (Exception e) {
				
				  salvarException("[registrarAgendamento] Nao foi possivel registrar o agendamento! patrimonio: "
				  +patrimonio + e.getMessage() + "\n" + e.getCause());
				 
			}
		}
		
		private void apenasCongelados(BigDecimal idAbastecimento, String patrimonio) throws Exception {
			if (idAbastecimento != null) {
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
						new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
							.wrapInterface(DynamicVO.class);

					String tecla = DynamicVO.asString("TECLA");
					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
					BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
					BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
					BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
					BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
					BigDecimal valorFinal = vlrpar.add(vlrfun);
					
					if (congelado(produto)) {
						try {

							EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
							EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
							DynamicVO VO = (DynamicVO) NPVO;

							VO.setProperty("ID", idAbastecimento);
							VO.setProperty("CODBEM", patrimonio);
							VO.setProperty("TECLA", tecla);
							VO.setProperty("CODPROD", produto);
							VO.setProperty("CAPACIDADE", capacidade);
							VO.setProperty("NIVELPAR", nivelpar);
							VO.setProperty("VALOR", valorFinal);

							dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

						} catch (Exception e) {
							
							  salvarException(
							  "[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
							  + e.getMessage() + "\n" + e.getCause());
							 
						}
					}
				}
			}
		}
		
		private void salvarException(String mensagem) {
			try {

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
				DynamicVO VO = (DynamicVO) NPVO;

				VO.setProperty("OBJETO", "acaoAgendada_agendaVisita");
				VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
				VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
				VO.setProperty("CODUSU", new BigDecimal(0));
				VO.setProperty("ERRO", mensagem);

				dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

			} catch (Exception e) {
				// aqui n�o tem jeito rs tem que mostrar no log
				System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
			}
		}
		
		//--- FIM FUN��ES
		
		// GET
		
		private int getRota(String patrimonio) {
			int count = 0;
			try {

				JdbcWrapper jdbcWrapper = null;
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
				ResultSet contagem;
				NativeSql nativeSql = new NativeSql(jdbcWrapper);
				nativeSql.resetSqlBuf();
				nativeSql.appendSql("SELECT ID FROM AD_ROTATEL WHERE ID IN (SELECT ID FROM AD_ROTATELINS WHERE codbem='"
						+ patrimonio + "') AND ROWNUM=1");
				contagem = nativeSql.executeQuery();
				while (contagem.next()) {
					count = contagem.getInt("ID");
				}

			} catch (Exception e) {
				salvarException("[getRota] Nao foi possibel obter a Rota! " + e.getMessage() + "\n" + e.getCause());
			}

			return count;
		}
		
		private BigDecimal getContrato(String patrimonio) throws Exception {
			BigDecimal contrato = null;
			try {
				JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
				DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
				contrato = VO.asBigDecimal("NUMCONTRATO");
			} catch (Exception e) {
				
				  salvarException("[getContrato] nao foi possivel obter o contrato, patrimonio:"
				  + patrimonio + "\n" + e.getMessage() + "\n" + e.getCause());
				 
			}
			
			if(contrato==null) {
				contrato = new BigDecimal(1314);
			}
			
			return contrato;
		}
		
		private BigDecimal getParceiro(String patrimonio) throws Exception {
			BigDecimal parceiro = null;
			BigDecimal contrato = null;
			
			try {
				JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
				DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });

				if(VO!=null) {
					contrato = VO.asBigDecimal("NUMCONTRATO");
				}
				
				if(contrato!=null) {
					DAO = JapeFactory.dao("Contrato");
					DynamicVO VOS = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
					parceiro = VOS.asBigDecimal("CODPARC");
				}
		
			} catch (Exception e) {
				
				  salvarException("[getParceiro] nao foi possivel obter o parceiro:" +
				  patrimonio + "\n" + e.getMessage() + "\n" + e.getCause());
				 
			}
			
			if(parceiro==null) {
				parceiro = new BigDecimal(1);
			}

			return parceiro;
		}
		
		private boolean congelado(BigDecimal codprod) throws Exception {
			boolean valida = false;

			JapeWrapper DAO = JapeFactory.dao("Produto");
			DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { codprod });
			BigDecimal grupo = VO.asBigDecimal("CODGRUPOPROD");

			DAO = JapeFactory.dao("GrupoProduto");
			DynamicVO VOS = DAO.findOne("CODGRUPOPROD=?", new Object[] { grupo });
			String congelado = VOS.asString("AD_CONGELADOS");

			if ("S".equals(congelado)) {
				valida = true;
			}
			return valida;
		}
		
		private void atualizarInstalacao(String patrimonio) {
			try {
				
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCInstalacao",
						"this.CODBEM=?", new Object[] { patrimonio }));
				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
					DynamicVO VO = (DynamicVO) NVO;

					VO.setProperty("AD_IDFLOW", null);
					VO.setProperty("PLANOGRAMAPENDENTE", "S");

					itemEntity.setValueObject(NVO);
				}
				
				
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		//--- FIM GET
		
		// VALIDA��ES
	
	}
